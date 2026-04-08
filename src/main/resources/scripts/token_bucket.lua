--[[
  Token Bucket Rate Limiter — Redis Lua Script

  How it works:
    - Each client gets a Redis Hash with two fields: 'tokens' and 'last_refill'
    - On each call: calculate elapsed time since last_refill, add proportional tokens
    - If tokens >= requested, consume and allow. Else deny.
    - TTL is set to prevent stale keys accumulating.

  KEYS[1]  = Redis key for this client (e.g. "rl:tb:user123")
  ARGV[1]  = capacity     (integer: max tokens)
  ARGV[2]  = refill_rate  (integer: tokens added per second)
  ARGV[3]  = now          (integer: current epoch milliseconds)
  ARGV[4]  = requested    (integer: tokens to consume, usually 1)

  Returns: { allowed (1|0), remaining_tokens (integer), capacity (integer) }
--]]

local key         = KEYS[1]
local capacity    = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now         = tonumber(ARGV[3])
local requested   = tonumber(ARGV[4])

-- TTL: enough time for the bucket to fully refill from empty + buffer
local ttl = math.ceil(capacity / refill_rate) + 10

-- Read current bucket state
local data       = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens     = tonumber(data[1])
local last_refill = tonumber(data[2])

if tokens == nil then
    -- First request: initialize full bucket
    tokens     = capacity
    last_refill = now
end

-- Refill: how many tokens to add based on elapsed time
local elapsed_ms   = math.max(0, now - last_refill)
local tokens_to_add = (elapsed_ms / 1000.0) * refill_rate
tokens = math.min(capacity, tokens + tokens_to_add)

-- Try to consume
local allowed   = 0
local remaining = math.floor(tokens)

if tokens >= requested then
    tokens    = tokens - requested
    remaining = math.floor(tokens)
    allowed   = 1
end

-- Persist updated state
redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
redis.call('EXPIRE', key, ttl)

return { allowed, remaining, capacity }
