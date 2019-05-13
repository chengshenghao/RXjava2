package com.example.csh.rxjava.operator;


import android.util.Log;

import java.util.ArrayList;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * 描述:变换操作符
 * <p/>作者：ss
 * <br/>创建时间：2019/5/13 11: 44
 */

public class Change {

    /**
     * 被观察者发送的每1个事件都通过 指定的函数 处理，从而变换成另外一种事件
     * 即， 将被观察者发送的事件转换为任意的类型事件。
     */
    public static void map() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
            }
        }).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return "使用Map变换操作符将事件" + integer + "的参数从 整型" + integer + " 变换成 字符串类型" + integer;
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i("csh", s);
            }
        });
    }

    /**
     * 作用：将被观察者发送的事件序列进行 拆分  & 单独转换，再合并成一个新的事件序列，最后再进行发送
     * 原理
     * <p>
     * 为事件序列中每个事件都创建一个 Observable 对象；
     * 将对每个 原始事件 转换后的 新事件 都放入到对应 Observable对象；
     * 将新建的每个Observable 都合并到一个 新建的、总的Observable 对象；
     * 新建的、总的Observable 对象 将 新合并的事件序列 发送给观察者（Observer）
     * <p>
     * 新合并生成的事件序列顺序是无序的，即 与旧序列发送事件的顺序无关
     * 具体案例参考餐饮7，登录
     */
    public static void flatMap() {
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
                ArrayList<String> arrayList = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    arrayList.add("转换" + integer);
                }
                return Observable.fromIterable(arrayList);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i("csh", "accept: " + s);
            }
        });
    }
    //ConcatMap（）作用：类似FlatMap（）操作符
    // 与FlatMap（）的 区别在于：拆分 & 重新合并生成的事件序列 的顺序 = 被观察者旧序列生产的顺序

//    Buffer（）
//
//    作用
//    定期从 被观察者（Obervable）需要发送的事件中 获取一定数量的事件 & 放到缓存区中，最终发送
}
