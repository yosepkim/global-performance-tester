package com.gpt.originredis.controller;

import com.gpt.originredis.model.KeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminController {
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/set/{key}")
    public String set(@PathVariable String key, @RequestBody KeyValue payload) {
        redisTemplate.opsForValue().set(key, payload.getValue());
        return "Successfully saved";
    }
    @GetMapping("/get/{key}")
    public KeyValue get(@PathVariable String key) {
        KeyValue response = new KeyValue();
        response.setKey(key);
        var value = redisTemplate.opsForValue().get(key);
        if (value != null) response.setValue(value.toString());
        else response.setValue("");
        return response;
    }
}
