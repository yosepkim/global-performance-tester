package com.gpt.originredis.controller;

import com.gpt.originredis.model.KeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;

@RestController
public class AdminController {
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/set/{key}")
    public String set(@PathVariable String key, @RequestBody KeyValue payload) {
        redisTemplate.opsForValue().set(key, payload.getValue());
        redisTemplate.opsForValue().set("time-" + key, Instant.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
        return "Successfully saved";
    }
    @GetMapping("/get/{key}")
    public KeyValue get(@PathVariable String key) throws InterruptedException {

        KeyValue response = new KeyValue();
        response.setKey(key);
        var value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            response.setValue(value.toString());
            response.setDatabaseInsertTime(Long.parseLong(redisTemplate.opsForValue().get("time-" + key).toString()));
        } else {
            response.setValue("");
        }
        return response;
    }
}
