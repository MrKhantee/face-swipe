package com.fleeting.service;

import org.bytedeco.javacpp.opencv_core.Point2fVectorVector;

/**
 * @author cxx
 * @date 2018/5/23
 */
public interface FaceFeatureService {

    /**
     * 提取人脸特征点
     * @return
     */
    Point2fVectorVector loadPoints(String fileName);

}
