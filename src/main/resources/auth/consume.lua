local digest = redis.call('HGET', KEYS[1], 'digest')
if not digest then return 0 end
if digest == ARGV[1] then redis.call('DEL', KEYS[1]); return 1 end
local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
if attempts >= tonumber(ARGV[2]) then redis.call('DEL', KEYS[1]) end
return 0
