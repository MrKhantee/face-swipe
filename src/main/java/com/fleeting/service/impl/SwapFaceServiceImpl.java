package com.fleeting.service.impl;

import com.fleeting.service.SwapFaceService;
import com.fleeting.util.MyUtils;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.opencv.core.CvType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;
import static org.bytedeco.javacpp.opencv_photo.NORMAL_CLONE;
import static org.bytedeco.javacpp.opencv_photo.seamlessClone;

/**
 * @author cxx
 * @date 2018/5/24
 */
@Service
public class SwapFaceServiceImpl implements SwapFaceService {

    @Resource
    private FaceFeatureServiceImpl faceFeatureService;

    @Override
    public String swapFace(String source, String target) {
        opencv_core.Mat sourceImg = imread(source);
        opencv_core.Mat targetImg = imread(target);
        opencv_core.Mat img1Warped = targetImg.clone();

        //这里只对图片中发现的其中一个脸处理
        opencv_core.Point2fVectorVector load = faceFeatureService.loadPoints(source);
        if (load == null){
            return "source 没有检测到人脸特征点";
        }
        opencv_core.Point2fVector point1 = load.get(0);
        opencv_core.Point2fVectorVector load1 = faceFeatureService.loadPoints(target);
        if (load1 == null){
            return "target 没有检测到人脸特征点";
        }
        opencv_core.Point2fVector point2 = load1.get(0);

        //转类型
        sourceImg.convertTo(sourceImg, CvType.CV_32F);
        img1Warped.convertTo(img1Warped,CvType.CV_32F);

        //计算凸包
        opencv_core.Mat hull = new opencv_core.Mat();
        convexHull(pvToMat(point2),hull,false,false);
        List<opencv_core.Point> hull1 = new ArrayList<>();
        List<opencv_core.Point> hull2 = new ArrayList<>();
        IntRawIndexer indexer = hull.createIndexer();
        for (int i = 0 ; i < indexer.height() ; i ++){
            int i11 = (int) point1.get(indexer.get(i)).x();
            int i12 = (int)  point1.get(indexer.get(i)).y();
            hull1.add(new opencv_core.Point(i11,i12));
            int i21 = (int)  point2.get(indexer.get(i)).x();
            int i22 = (int)  point2.get(indexer.get(i)).y();
            hull2.add(new opencv_core.Point(i21,i22));
        }

        opencv_core.Rect rect = new opencv_core.Rect(0,0,img1Warped.cols(),img1Warped.rows());
        List<int[]> dt = calculateDelaunayTriangles(rect, hull2);

        for (int i = 0 ; i < dt.size(); i++){
            opencv_core.Mat t1 = new opencv_core.Mat(3,2,CvType.CV_32S);
            opencv_core.Mat t2 = new opencv_core.Mat(3,2,CvType.CV_32S);
            IntRawIndexer t1Indexer = t1.createIndexer();
            IntRawIndexer t2Indexer = t2.createIndexer();
            int[] ints = dt.get(i);
            //Get points for img1, img2 corresponding to the triangles
            //获取 source 和 target 中相应的三角形的点
            for (int j = 0;j < 3;j++){
                opencv_core.Point dp1 = hull1.get(ints[j]);
                t1Indexer.put(j,0,dp1.x());
                t1Indexer.put(j,1,dp1.y());

                opencv_core.Point dp2 = hull2.get(ints[j]);
                t2Indexer.put(j,0,dp2.x());
                t2Indexer.put(j,1,dp2.y());
            }
            warpTriangle(sourceImg,img1Warped,t1,t2);
        }
        opencv_core.Mat t2 = new opencv_core.Mat(hull2.size(),2,CvType.CV_32S);
        IntRawIndexer t2Indexer = t2.createIndexer();
        for (int j = 0;j < hull2.size();j++){
            t2Indexer.put(j,0,hull2.get(j).x());
            t2Indexer.put(j,1,hull2.get(j).y());
        }
        opencv_core.Mat mast = opencv_core.Mat.zeros(targetImg.rows(), targetImg.cols(), targetImg.depth()).asMat();
        fillConvexPoly(mast,t2,new opencv_core.Scalar(255,255,255,0));

        opencv_core.Mat hull2Mat = new opencv_core.Mat(hull2.size(),2,CvType.CV_32S);
        IntRawIndexer hull2MatIndexer = hull2Mat.createIndexer();
        for (int i = 0 ; i < hull2.size(); i++){
            hull2MatIndexer.put(i,0,hull2.get(i).x());
            hull2MatIndexer.put(i,1,hull2.get(i).y());
        }
        opencv_core.Rect r = boundingRect(hull2Mat);
        opencv_core.Point tl = r.tl();
        opencv_core.Point br = r.br();
        opencv_core.Point center = new opencv_core.Point((tl.x() + br.x()) / 2, (tl.y() + br.y()) / 2);

        opencv_core.Mat output = new opencv_core.Mat();

        img1Warped.convertTo(img1Warped,CV_8UC3);

        Indexer indexer3 = img1Warped.createIndexer();

        //img1Warped ：source 的脸 覆盖在 target 上
        //targetImg  ：要替换人脸的图片
        //mast ：黑底，人脸部分为白底
        //output ：替换结果
        seamlessClone(img1Warped,targetImg,mast,center,output,NORMAL_CLONE);
        String url = MyUtils.getAbsolueUrl("static/" + MyUtils.generateString(10) + ".png");
        imwrite(url,output);
        return url;
    }


    private opencv_core.Mat pvToMat(opencv_core.Point2fVector pv){
        int size = (int) pv.size();
        opencv_core.Mat mat = new opencv_core.Mat(size,2 ,CvType.CV_32F);
        FloatRawIndexer indexer = mat.createIndexer();
        for (Integer i = 0 ;i < size;i++){
            opencv_core.Point2f ints = pv.get(i);
            indexer.put(i,0, ints.x());
            indexer.put(i,1, ints.y());
        }
        return mat;
    }

    /**
     * 在获得的凸包上做 德劳内三角 切分
     * @param rect
     * @param points
     * @return
     */
    private List<int[]> calculateDelaunayTriangles(opencv_core.Rect rect, List<opencv_core.Point> points){
        opencv_imgproc.Subdiv2D subdiv = new opencv_imgproc.Subdiv2D(rect);
        for (opencv_core.Point point : points){
            subdiv.insert(new opencv_core.Point2f(point.x(),point.y()));
        }
        FloatPointer floatPointer = new FloatPointer();
        //获得三角形的点的集合，其中每6位表示一个三角形
        subdiv.getTriangleList(floatPointer);
        List<int[]> resultList = new ArrayList<>();
        for ( int i = 0 ; i < floatPointer.limit() ; i++){
            opencv_core.Point[] pt = new opencv_core.Point[3];
            int[] ind = new int[3];
            pt[0] = new opencv_core.Point((int) floatPointer.get(get6Index(i,0)),(int) floatPointer.get(get6Index(i,1)));
            pt[1] = new opencv_core.Point((int) floatPointer.get(get6Index(i,2)),(int) floatPointer.get(get6Index(i,3)));
            pt[2] = new opencv_core.Point((int) floatPointer.get(get6Index(i,4)),(int) floatPointer.get(get6Index(i,5)));

            if (rect.contains(pt[0]) && rect.contains(pt[1]) && rect.contains(pt[2])){
                for (int j= 0; j < 3 ; j ++){
                    for (int k = 0 ; k < points.size();k++){
                        if (Math.abs(pt[j].x() - points.get(k).x()) < 1 &&
                                Math.abs(pt[j].y() - points.get(k).y()) < 1){
                            ind[j] = k;
                        }
                    }
                }
                resultList.add(ind);
            }
        }
        return resultList;
    }

    /**
     * 一个三角形有3个点，每个点用两个数据表示，所以一个三角形需要6个数据
     * @param i
     * @param index
     * @return
     */
    private int get6Index(int i,int index){
        return  index + i * 6;
    }


    /**
     *
     * @param img1
     * @param img2
     * @param t1
     * @param t2
     */
    private void warpTriangle(opencv_core.Mat img1 , opencv_core.Mat img2, opencv_core.Mat t1 , opencv_core.Mat t2){
        Rect r1 = boundingRect(t1);
        Rect r2 = boundingRect(t2);

        opencv_core.Mat t1Rect = new opencv_core.Mat(3,2,CvType.CV_32F);
        opencv_core.Mat t2Rect = new opencv_core.Mat(3,2,CvType.CV_32F);
        opencv_core.Mat t2RectInt = new opencv_core.Mat(3,2,CvType.CV_32S);
        IntRawIndexer t2RectIntIndexer = t2RectInt.createIndexer();
        FloatRawIndexer t1RectIndexer = t1Rect.createIndexer();
        FloatRawIndexer t2RectIndexer = t2Rect.createIndexer();
        for (int i = 0 ; i < 3 ; i++){
            IntRawIndexer t1Indexer = t1.createIndexer();
            IntRawIndexer t2Indexer = t2.createIndexer();
            t2RectIntIndexer.put(i,0,t2Indexer.get(i,0) - r2.x());
            t2RectIntIndexer.put(i,1,t2Indexer.get(i,1) - r2.y());

            t2RectIndexer.put(i,0,t2Indexer.get(i,0) - r2.x());
            t2RectIndexer.put(i,1,t2Indexer.get(i,1) - r2.y());

            t1RectIndexer.put(i,0,t1Indexer.get(i,0) - r1.x());
            t1RectIndexer.put(i,1,t1Indexer.get(i,1) - r1.y());
        }

        opencv_core.Mat mask = opencv_core.Mat.zeros(r2.height(), r2.width(), CvType.CV_32FC3).asMat();

        fillConvexPoly(mask,t2RectInt,new opencv_core.Scalar(1,1,1,0),16,0);

        opencv_core.Mat img1Rect = new opencv_core.Mat();

        img1.apply(r1).copyTo(img1Rect);

        opencv_core.Mat img2Rect = opencv_core.Mat.zeros(r2.height(),r2.width(),img1Rect.type()).asMat();

        applyAffineTransform(img2Rect,img1Rect,t1Rect,t2Rect);

        multiply(img2Rect,mask,img2Rect);

        img2 = img2.apply(r2);
        MatExpr subtract = subtract(new opencv_core.Scalar(1, 1, 1, 0),mask);
        multiply(img2,subtract.asMat(), img2);

        addPut(img2,img2Rect);
    }

    /**
     * 对图像进行仿射变换
     * @param warpImage
     * @param src
     * @param srcTri
     * @param dstTri
     */
    private void applyAffineTransform(Mat warpImage, Mat src, Mat srcTri, Mat dstTri){
        Mat warpMat = getAffineTransform(srcTri, dstTri);
        warpAffine(src, warpImage, warpMat, warpImage.size() ,INTER_LINEAR, BORDER_REFLECT_101,new Scalar(0,0,0,0));
    }
}
