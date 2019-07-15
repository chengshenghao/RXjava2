package com.example.csh.rxjava;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "rxandroid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        case01();
//        case02();
//        case03();
//        case04();
        case05();
    }

    /**
     * 过滤操作符
     */
    private void case05() {
        //filter()过滤 特定条件的事件
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onNext(4);
            }
        }).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
                return integer > 3;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.i(TAG, "accept: " + integer);
            }
        });
        //ofType 过滤 特定数据类型的数据
        Observable.just("1", 2, 3)
                .ofType(Integer.class)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.i(TAG, "accept: " + integer);
                    }
                });
        //skip（） / skipLast（）跳过某个事件

        //distinct（） / distinctUntilChanged（）过滤事件序列中重复的事件 / 连续重复的事件
        Observable.just(1, 1, 1, 2, 3, 4, 5, 6)
                .distinct()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.i(TAG, "accept: " + integer);
                    }
                });
        //take（） & takeLast（）通过设置指定的事件数量，仅发送特定数量的事件
        //throttleFirst（）/ throttleLast（）在某段时间内，只发送该段时间内第1次事件 / 最后1次事件
        //Sample（）在某段时间内，只发送该段时间内最新（最后）1次事件

        //throttleWithTimeout （） / debounce（）发送数据事件时，若2次发送事件的间隔＜指定时间，
        //就会丢弃前一次的数据，直到指定时间内都没有新数据发射时才会发送后一次的数据

        //firstElement（） / lastElement（）仅选取第1个元素 / 最后一个元素
        //elementAt（） 指定接收某个元素（通过 索引值 确定）
        //elementAtOrError（）在elementAt（）的基础上，当出现越界情况（即获取的位置索引 ＞ 发送事件序列长度）时，即抛出异常
    }


    /**
     * 功能性操作符（不再赘述）
     */
    private void case04() {

    }

    /**
     * 组合、合并数据(暂时不学习)
     */
    private void case03() {
        //concat()组合多个被观察者一起发送数据，合并后 按发送顺序串行执行
        //二者区别：组合被观察者的数量，即concat（）组合被观察者数量≤4个，而concatArray（）则可＞4个
        Observable.concat(Observable.just(1),
                Observable.just(2),
                Observable.just(3),
                Observable.just(4))
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.i(TAG, "accept: " + integer);
                    }
                });
        //merge（） / mergeArray（）组合多个被观察者一起发送数据，合并后 按时间线并行执行
        //collect()将被观察者Observable发送的数据事件收集到一个数据结构里
        Observable.just("1", "2", "3")
                .collect(() -> new ArrayList(), (arrayList, o) -> arrayList.add(o))
                .subscribe(new Consumer<ArrayList>() {
                    @Override
                    public void accept(ArrayList arrayList) throws Exception {
                        Log.i(TAG, "accept: " + arrayList);
                    }
                });
        //count()统计被观察者发送事件的数量
        Observable.just(1, 2, 3, 4, 5)
                .count()
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.i(TAG, "发送的事件数量 =  " + aLong);

                    }
                });
    }

    /**
     * 变换操作符
     */
    private void case02() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
            }
        }).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return "我是" + integer;
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i(TAG, "accept: " + s);
            }
        });
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
            }
        }).flatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                //可用于网络请求的嵌套回调，注册请求完，进行登录请求操作
                return Observable.fromArray("1", "2");
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i(TAG, "accept: " + s);
            }
        });
        //ConcatMap同上，有序
        //buffer 的使用
    }

    /**
     * 创建操作符
     * 文章存在错误问题2.0 Observable.subscribe 订阅不上Subcriber的。
     */
    private void case01() {
        Observable.create((ObservableEmitter<Integer> emitter) -> {//改为lambda方式
            emitter.onNext(4);
            emitter.onNext(5);
            emitter.onNext(6);
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "subscribe");
            }

            @Override
            public void onNext(Integer value) {
                Log.d(TAG, "" + value);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "error");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "complete");
            }
        });
        //方法1：just(T...)：直接将传入的参数依次发送出来
        Observable<String> observable = Observable.just("a", "b", "c");
        observable.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String value) {
                Log.i(TAG, "onNext: " + value);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        String[] words = {"A", "B", "C"};
        Observable.fromArray(words).subscribe(new Observer<String>() {

            private Disposable disposable;

            @Override
            public void onSubscribe(Disposable d) {
                disposable = d;

            }

            @Override
            public void onNext(String value) {
                Log.i(TAG, value);
                if (value.equals("B")) {
                    //可采用 Disposable.dispose() 切断观察者 与 被观察者 之间的连接
                    disposable.dispose();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
//        以 Consumer为例：实现简便式的观察者模式
        Observable.just("hello").subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i(TAG, "accept: " + s);
            }
        });
        Observable.timer(5, TimeUnit.SECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG, "onSubscribe: ");
                    }

                    @Override
                    public void onNext(Long value) {
                        Log.i(TAG, "onNext: 5秒后接受到的数据");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        Observable.interval(2, 5, TimeUnit.SECONDS).subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i(TAG, "onSubscribe: ");
            }

            @Override
            public void onNext(Long value) {
                Log.i(TAG, "onNext: 每五秒接受一次");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        //intervalRange(),range(),rangeLong()具体使用参考案例

    }
}
