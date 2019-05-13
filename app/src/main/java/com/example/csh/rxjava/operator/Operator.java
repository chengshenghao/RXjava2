package com.example.csh.rxjava.operator;


import android.util.Log;

import com.example.csh.rxjava.bean.Translation;
import com.example.csh.rxjava.http.GetRequest_Interface;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 描述:创建操作符使用
 * <p/>作者：ss
 * <br/>创建时间：2019/5/13 10: 18
 */

public class Operator {
    private static int i = 1;

    /**
     * 基本使用
     */
    public static void base() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer value) {
                Log.i("csh", "onNext: " + value);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public static void just() {
        Observable.just(1, 2, 3)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer value) {
                        Log.i("csh", "just: " + value);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public static void fromArray() {
        Integer[] arr = {1, 2, 3, 4, 5};
        Observable.fromArray(arr).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer value) {
                Log.i("csh", "fromArray: " + value);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public static void OtherCreat() {
        //creat just fromArray略
        // 设置一个集合
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Observable.fromIterable(list).subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer integer) throws Exception {
                Log.i("csh", "accept: " + integer);
            }
        });
//        通过defer 定义被观察者对象
        // 注：此时被观察者对象还没创建，开始订阅之后才会被创建
        Observable observable = Observable.defer(new Callable<ObservableSource<?>>() {
            @Override
            public ObservableSource<?> call() throws Exception {
                return Observable.just(i);
            }
        });
        i = 2;
        // 注：此时，才会调用defer（）创建被观察者对象（Observable）
//        observable.subscribe(new Consumer() {
//            @Override
//            public void accept(Object o) throws Exception {
//                Log.i(TAG, "接收到数据: "+o);
//            }
//        });
//        observable.timer(2, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
//            @Override
//            public void accept(Long aLong) throws Exception {
//                Log.i(TAG, "accept: "+aLong);
//            }
//        });

//        快速创建1个被观察者对象（Observable）
//        发送事件的特点：每隔指定时间 就发送 事件
//        observable.interval(2,TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
//            @Override
//            public void accept(Long aLong) throws Exception {
//                Log.i(TAG, "accept: "+aLong);
//            }
//        });

//        快速创建1个被观察者对象（Observable）
//        发送事件的特点：每隔指定时间 就发送 事件，可指定发送的数据的数量
//        observable.intervalRange(1,10,5,1,TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
//            @Override
//            public void accept(Long aLong) throws Exception {
//                Log.i(TAG, "accept: "+aLong);
//            }
//        });


//        快速创建1个被观察者对象（Observable）
//        发送事件的特点：连续发送 1个事件序列，可指定范围
//        observable.range(1,20).subscribe(new Consumer<Integer>() {
//            @Override
//            public void accept(Integer integer) throws Exception {
//                Log.i(TAG, "accept: "+integer);
//            }
//        });
//        rangeLong（）
//        作用：类似于range（），区别在于该方法支持数据类型 = Long
//        具体使用
//        与range（）类似，此处不作过多描述

    }

    /**
     * 网络请求轮训
     */
    public static void netWorkDemo() {
        Observable.interval(2, 1, TimeUnit.SECONDS)
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.i("csh", " 第" + aLong + "次轮询");
                        //进行网络请求
                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl("http://fy.iciba.com/") // 设置 网络请求 Url
                                .addConverterFactory(GsonConverterFactory.create()) //设置使用Gson解析(记得加入依赖)
                                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // 支持RxJava
                                .build();

                        // b. 创建 网络请求接口 的实例
                        GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);

                        // c. 采用Observable<...>形式 对 网络请求 进行封装
                        Observable<Translation> observable = request.getCall();
                        // d. 通过线程切换发送网络请求
                        observable.subscribeOn(Schedulers.io())               // 切换到IO线程进行网络请求
                                .observeOn(AndroidSchedulers.mainThread())  // 切换回到主线程 处理请求结果
                                .subscribe(new Observer<Translation>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {
                                    }

                                    @Override
                                    public void onNext(Translation result) {
                                        // e.接收服务器返回的数据
                                        result.show();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.d("csh", "请求失败");
                                    }

                                    @Override
                                    public void onComplete() {

                                    }
                                });

                    }
                }).subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }
}
