if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
redis.call('HSET', KEYS[2], 'digest', ARGV[2], 'attempts', 0)
redis.call('EXPIRE', KEYS[2], ARGV[3])
return 1
