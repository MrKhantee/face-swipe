package com.fleeting;

import com.fleeting.service.SwapFaceService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FaceSwipeApplicationTests {

	@Resource
	private SwapFaceService swapFaceService;

	@Test
	public void contextLoads() {
		String path = swapFaceService.swapFace("/Users/cxx/baby.png", "/Users/cxx/baby4.png");
		System.out.println(path);
	}

}
