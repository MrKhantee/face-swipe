package com.fleeting.service;

/**
 * @author cxx
 * @date 2018/5/23
 */
public interface SwapFaceService {

    /**
     * 将 source 替换到 target 上
     * @param source
     * @param target
     * @return 返回替换脸后的 target 图片的文件名
     */
    String swapFace(String source,String target);

}
