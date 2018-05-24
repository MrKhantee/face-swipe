package com.fleeting.config;

import com.fleeting.util.MyUtils;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_objdetect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author cxx
 * @date 2018/5/24
 */
@Configuration
public class BeanConfig {

    @Bean
    public opencv_face.FacemarkLBF faceMark(){
        opencv_face.FacemarkLBF facemark = opencv_face.FacemarkLBF.create();
        facemark.loadModel(MyUtils.getAbsolueUrl("config/lbfmodel.yaml"));
        return facemark;
    }

    @Bean
    public opencv_objdetect.CascadeClassifier cascadeClassifier(){
        return new opencv_objdetect.CascadeClassifier(MyUtils.getAbsolueUrl("config/haarcascade_frontalface_alt2.xml"));
    }
}
