--[[
  Sliding Window Rate Limiter — Redis Lua Script

  How it works:
    - Each client gets a Redis Sorted Set where score = request timestamp (ms)
    - On each call: remove entries older than (now - window_ms), count remaining
    - If count < limit, add new entry and allow. Else deny.

  KEYS[1]  = Redis key for this client (e.g. "rl:sw:user123")
  ARGV[1]  = window_ms  (integer: window size in milliseconds)
  ARGV[2]  = limit      (integer: max requests per window)
  ARGV[3]  = now        (integer: current epoch milliseconds)
  ARGV[4]  = request_id (string: unique ID for this request, prevents score collisions)

  Returns: { allowed (1|0), remaining (integer), limit (integer) }
--]]

local key        = KEYS[1]
local window_ms  = tonumber(ARGV[1])
local limit      = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])
local request_id = ARGV[4]

local window_start = now - window_ms

-- Evict all entries older than the window start
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

-- Count how many requests are in the current window
local count = redis.call('ZCARD', key)

local allowed   = 0
local remaining = limit - count

if count < limit then
    -- Add this request to the window with timestamp as score
    -- Using request_id as member ensures uniqueness even for same-millisecond requests
    redis.call('ZADD', key, now, request_id)
    allowed   = 1
    remaining = remaining - 1
end

-- Key expires after one full window of inactivity
redis.call('PEXPIRE', key, window_ms)

return { allowed, remaining, limit }
