package com.fleeting.controller;

import com.fleeting.service.SwapFaceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author cxx
 * @date 2018/5/24
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @Resource
    private SwapFaceService swapFaceService;

    @ResponseBody
    @RequestMapping("/faceswape")
    public String test(String source , String target){
        return swapFaceService.swapFace(source,target);
    }
}
