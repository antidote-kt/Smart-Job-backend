package com.sysu.smartjob.result;

import com.sysu.smartjob.constant.ResultCodeConstant;
import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; //编码：1成功，0和其它数字为失败
    private String msg; //信息
    private T data; //数据


    /**
     * 返回成功结果，带消息，无数据
     */
    public static <T> Result<T> success(String msg) {
        Result<T> result = new Result<T>();
        result.code = ResultCodeConstant.SUCCESS;
        result.msg = msg;
        return result;
    }

    /**
     * 返回成功结果，同时带数据和消息
     */
    public static <T> Result<T> success(T object, String msg) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.msg = msg;
        result.code = ResultCodeConstant.SUCCESS;
        return result;
    }

    /**
     * 返回错误结果，带错误信息
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<T>();
        result.msg = msg;
        result.code = ResultCodeConstant.ERROR;
        return result;
    }

}
