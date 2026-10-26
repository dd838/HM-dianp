--1.参数列表
--1.1.优惠券id
local voucherId = ARGV[1]
--1.2.用户id
local userId = ARGV[2]

--2.数据key
--2.1.库存key
local stockKey = "secKill:stock:" .. voucherId
--2.2.订单key
local userKey = "secKill:stock:" .. voucherId

--3.脚本业务
--3.1.判断库存是否充足 get stockKey
if(toNumber(redis.call('get',stockKey)) <= 0) then
        --3.2.库存不足，返回1
    return 1
    end

--3.2.判断用户是否下单sisMember orderKey userId
if(redis.call('sisMember',orderKey,userId) == 1) then
    --3.3.存在说明重复下单，返回2
    return 2
    end

--3.4.扣库存incrby stockKey -1
redis.call('incrby',stockKey,-1)
--3.4.下单（保存用户）sadd orderKey userId
redis.call('sadd',orderKey,userKey)