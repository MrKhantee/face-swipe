package com.fleeting.service.impl;

import com.fleeting.service.FaceFeatureService;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_objdetect;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

/**
 * @author cxx
 * @date 2018/5/24
 */
@Service
public class FaceFeatureServiceImpl implements FaceFeatureService{

    @Resource
    private opencv_face.FacemarkLBF faceMark;

    @Resource
    private opencv_objdetect.CascadeClassifier cascadeClassifier;

    @Override
    public opencv_core.Point2fVectorVector loadPoints(String fileName) {
        //对应 c++ 中 std::vector<cv::Rect> 类型
        opencv_core.RectVector faces = new opencv_core.RectVector();
        //读取图片到 mat 中
        opencv_core.Mat gray = imread(fileName);
        //获取面目特征感兴趣区域
        cascadeClassifier.detectMultiScale(gray,faces);
        //对应 c++ 中 std::vector<std::vector<cv::Point2f> > ， 用来保存结果
        opencv_core.Point2fVectorVector pp = new opencv_core.Point2fVectorVector();
        //获取n个人脸的面目特征点 68个点，并保存在 pp 中列表中
        boolean success = faceMark.fit(gray, new opencv_core.Mat(faces),new opencv_core.Mat(pp));
        if (success){
            return pp;
        }else{
            return null;
        }
    }
}
