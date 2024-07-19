package com.nomz.doctor_study.jwt.response;

import com.ssafy.common.model.response.BaseResponseBody;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 유저 로그인 API ([POST] /api/v1/auth) 요청에 대한 응답값 정의.
 */
@Getter
@Setter
@ApiModel("UserLoginPostResponse")
public class UserLoginPostRes extends BaseResponseBody{
	@ApiModelProperty(name="JWT 인증 토큰", example="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN...")
	String accessToken;
	String refreshToken;
	
	public static UserLoginPostRes of(Integer statusCode, String message, Map<String, String> tokens) {
		UserLoginPostRes res = new UserLoginPostRes();
		res.setStatusCode(statusCode);
		res.setMessage(message);
		res.setAccessToken(tokens.get("accessToken"));
		res.setRefreshToken(tokens.get("refreshToken"));

		return res;
	}
}
