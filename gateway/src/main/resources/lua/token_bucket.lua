-- Token Bucket Rate Limiter
-- KEYS[1]  = bucket key (e.g. "rl:{apiKeyValue}")
-- ARGV[1]  = capacity  (calls_per_sec)
-- ARGV[2]  = now       (current time in milliseconds)
--
-- Tokens stored as millitokens (x1000) to avoid floating-point in Redis.
-- Returns 1 = allowed, 0 = rate limited.

local key      = KEYS[1]
local capacity = tonumber(ARGV[1]) * 1000
local now      = tonumber(ARGV[2])

local data   = redis.call('HMGET', key, 'ts', 'tk')
local last_ts = tonumber(data[1])
local tokens  = tonumber(data[2])

if last_ts == nil then
    tokens = capacity - 1000
    redis.call('HMSET', key, 'ts', now, 'tk', tokens)
    redis.call('EXPIRE', key, 60)
    return 1
end

local elapsed_ms = now - last_ts
local refill     = math.floor(elapsed_ms * capacity / 1000)
tokens = math.min(capacity, tokens + refill)

if tokens >= 1000 then
    tokens = tokens - 1000
    redis.call('HMSET', key, 'ts', now, 'tk', tokens)
    redis.call('EXPIRE', key, 60)
    return 1
else
    redis.call('HMSET', key, 'ts', now, 'tk', tokens)
    redis.call('EXPIRE', key, 60)
    return 0
end
