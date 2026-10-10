"""Exercise shipped Lua against local Redis, with unique temporary keys only.
Run: python scripts/verify_email_auth_redis.py [host] [port]
Never scans/flushes Redis or touches application keys.
"""
import socket
import sys
import time
import uuid
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
HOST = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 6379
PREFIX = "email-auth-test:" + uuid.uuid4().hex + ":"
KEYS = [PREFIX + str(i) for i in range(20)]
SCRIPTS = Path(__file__).resolve().parents[1] / "src/main/resources/auth"
def command(*args):
    with socket.create_connection((HOST, PORT), timeout=3) as conn:
        parts = [str(arg).encode() for arg in args]
        conn.sendall(b"*%d\r\n" % len(parts) + b"".join(b"$%d\r\n" % len(part) + part + b"\r\n" for part in parts))
        stream = conn.makefile("rb")
        first = stream.readline().rstrip(b"\r\n")
        if first[:1] == b"-": raise RuntimeError(first.decode())
        if first[:1] == b":": return int(first[1:])
        if first[:1] == b"$":
            size = int(first[1:])
            return None if size < 0 else stream.read(size).decode()
        return first[1:].decode()
def evaluate(name, keys, *args):
    return command("EVAL", (SCRIPTS / (name + ".lua")).read_text(), len(keys), *keys, *args)
def publish(key, digest="digest", ttl=300):
    command("HSET", key, "digest", digest, "attempts", 0)
    command("EXPIRE", key, ttl)
try:
    assert command("PING") == "PONG"
    reservation = KEYS[:4]
    assert evaluate("reserve", reservation, "owner", 60, 10, 30) == 1
    assert evaluate("reserve", reservation, "other", 60, 10, 30) == 0
    assert evaluate("consume", [KEYS[3]], "digest", 5) == 0  # pending SMTP is invalid
    assert evaluate("publish", [KEYS[0], KEYS[3]], "wrong-owner", "digest", 300) == 0
    assert evaluate("publish", [KEYS[0], KEYS[3]], "owner", "digest", 300) == 1
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(lambda _: evaluate("consume", [KEYS[3]], "digest", 5), range(20)))
    assert sum(results) == 1
    publish(KEYS[4])
    for _ in range(5): assert evaluate("consume", [KEYS[4]], "wrong", 5) == 0
    assert evaluate("consume", [KEYS[4]], "digest", 5) == 0
    publish(KEYS[5], ttl=1)
    assert evaluate("consume", [KEYS[6]], "digest", 5) == 0  # purpose/token isolation
    time.sleep(1.1)
    assert evaluate("consume", [KEYS[5]], "digest", 5) == 0
    assert evaluate("release", reservation, "other") == 0
    assert evaluate("release", reservation, "owner") == 1
    assert command("GET", KEYS[1]) == "0"
    assert command("GET", KEYS[0]) is None
    assert evaluate("reserve", reservation, "new-owner", 60, 1, 1) == 1
    assert evaluate("publish", [KEYS[0], KEYS[3]], "new-owner", "new-digest", 300) == 1
    command("DEL", KEYS[0])
    assert evaluate("reserve", reservation, "limit-owner", 60, 1, 1) == 0
    assert evaluate("consume", [KEYS[3]], "digest", 5) == 0
    assert evaluate("consume", [KEYS[3]], "new-digest", 5) == 1
    print("PASS: quotas, cooldown, pending publish, ownership, rollback, single concurrent consume, attempts, expiry, isolation, replacement")
finally:
    command("DEL", *KEYS)
