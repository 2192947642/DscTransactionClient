package com.lgzClient.redis;


import com.alibaba.nacos.common.utils.StringUtils;
import com.lgzClient.annotations.DCSTransaction;
import com.lgzClient.types.GlobalType;
import com.lgzClient.types.LocalType;
import com.lgzClient.types.status.GlobalStatus;
import com.lgzClient.types.status.LocalStatus;
import com.lgzClient.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Component
public class TransactSqlRedisHelper {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public void addBranchTransaction(LocalType globalTransaction){
        stringRedisTemplate.opsForHash().put(globalTransaction.getGlobalId(), globalTransaction.getLocalId(),JsonUtil.objToJson(globalTransaction));
    }
    public void updateGlobalStatus(String globalId, GlobalStatus status){
        stringRedisTemplate.opsForHash().put(globalId,"status",JsonUtil.objToJson(status));
    }

    public GlobalType getGlobalType(String globalId){
        Map<Object,Object> map= stringRedisTemplate.opsForHash().entries(globalId);
        String status=map.get("status").toString();//
        map.remove("status");
        HashMap<String,LocalType> typeMaps=new HashMap<>();
        for(Object key:map.keySet()){
            LocalType localType=JsonUtil.jsonToObject(map.get(key).toString(), LocalType.class);
            typeMaps.put(key.toString(),localType);
        }
        GlobalStatus globalStatus=null;
        if(StringUtils.hasLength(status)){//如果当前有状态
            globalStatus=JsonUtil.jsonToObject(status,GlobalStatus.class);
        }
        //如果当前还没有确定最终结果 那么就进行进一步判断
        if(globalStatus==null||globalStatus==GlobalStatus.wait){
            globalStatus=null;
            for(String key:typeMaps.keySet()){
                LocalType localType=typeMaps.get(key);
                if(localType.getStatus() == LocalStatus.wait){
                    globalStatus=GlobalStatus.wait;
                }
                else if(localType.getStatus() == LocalStatus.fail){
                    globalStatus=GlobalStatus.fail;
                }
            }
            if(globalStatus==null){//如果不是fail 或者wait
                globalStatus=GlobalStatus.success;
            }
            updateGlobalStatus(globalId,globalStatus);//更新redis中的分布式事务状态
        }
        GlobalType globalType=new GlobalType(globalStatus,typeMaps);
        return  globalType;
    }

    //修改redis中本地的事务状态
    public void updateLocalTransaction(LocalType localType){
        if(stringRedisTemplate.opsForHash().hasKey(localType.getGlobalId(), localType.getLocalId())){

            stringRedisTemplate.opsForHash().put(localType.getGlobalId(), localType.getLocalId(),JsonUtil.objToJson(localType));
        }
    }
    //删除redis中本地的事务记录
    public void deleteLocalTransaction(String globalId,String localId ){

        stringRedisTemplate.opsForHash().delete(globalId,localId);
    }
    public void deleteLocalTransactionWithDeleteGlobal(String globalId,String localId){
        // 定义Lua脚本
        String luaScript =
                """
                     local globalId = KEYS[1]
                     local localId = ARGV[1]
                     if redis.call('HEXISTS', globalId, localId) == 1 then
                         redis.call('HDEL', globalId, localId)
                         -- 检查哈希表中是否只剩下 'status' 键或者是否为空
                         local remaining_keys = redis.call('HKEYS', globalId)
                         if #remaining_keys == 0 or (#remaining_keys == 1 and remaining_keys[1] == 'status') then
                             redis.call('DEL', globalId)
                         end
                     end
                     return nil
                """;
        // 创建RedisScript对象
        RedisScript<Void> script = new DefaultRedisScript<>(luaScript, Void.class);
        // 执行脚本
        stringRedisTemplate.execute(script, Collections.singletonList(globalId), localId);
    }
    public LocalType getLocalTransaction(String globalId,String localId){
        Object object=stringRedisTemplate.opsForHash().get(globalId,localId);
        if(object==null) return null;
        else return JsonUtil.jsonToObject(object.toString(),LocalType.class);
    }
    //删除分布式事务的记录
    public void deleteGlobalTransaction(String globalId){
        stringRedisTemplate.delete(globalId);
    }

}
