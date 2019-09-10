package com.code.common.entity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * create by liuliang
 * on 2019-08-29  09:32
 */
public class CrawlerTest {
    public static void main(String[] args) {
//        Integer total = 0;
//        check(total);
//        System.out.println(total);


//        String[] arr = new String[]{"1","2"};
//        List<String> list = Arrays.asList(arr);
//
//        System.out.println(list.size());


        System.out.println(find());
        Executors.newFixedThreadPool(1);

    }


    public static boolean find(){
        try {
            return true;
        }catch (Exception e){
            return true;
        }finally {
            return false;
        }
    }


    private static void check(Integer total){
        if(total<1){
            total = total+1;
        }
        System.out.println("==="+total);
    }
}
