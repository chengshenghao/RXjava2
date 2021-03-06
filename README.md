#教程一
## 1.添加配置信息
	要在Android中使用RxJava2, 先添加Gradle配置:
    compile 'io.reactivex.rxjava2:rxjava:2.0.1'
    compile 'io.reactivex.rxjava2:rxandroid:2.0.1'

## 2.RxJava的基本工作原理
这里的上游和下游就分别对应着RxJava中的Observable和Observer，它们之间的连接就对应着subscribe()，因此这个关系用RxJava来表示就是：

	 Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }
        })
                .subscribe(new Observer<Integer>() {
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
		12-02 03:37:17.818 4166-4166/zlc.season.rxjava2demo D/TAG: subscribe 
		12-02 03:37:17.819 4166-4166/zlc.season.rxjava2demo D/TAG: 1 
		12-02 03:37:17.819 4166-4166/zlc.season.rxjava2demo D/TAG: 2 
		12-02 03:37:17.819 4166-4166/zlc.season.rxjava2demo D/TAG: 3 
		12-02 03:37:17.819 4166-4166/zlc.season.rxjava2demo D/TAG: complete

注意: 只有当上游和下游建立连接之后, 上游才会开始发送事件. 也就是调用了subscribe() 方法之后才开始发送事件.

## 3.ObservableEmitter和Disposable.
ObservableEmitter： Emitter是发射器的意思，那就很好猜了，这个就是用来发出事件的，它可以发出三种类型的事件，通过调用emitter的onNext(T value)、onComplete()和onError(Throwable error)就可以分别发出next事件、complete事件和error事件。
但是，请注意，并不意味着你可以随意乱七八糟发射事件，需要满足一定的规则：

    上游可以发送无限个onNext, 下游也可以接收无限个onNext.
    当上游发送了一个onComplete后, 上游onComplete之后的事件将会继续发送, 而下游收到onComplete事件之后将不再继续接收事件.
    当上游发送了一个onError后, 上游onError之后的事件将继续发送, 而下游收到onError事件之后将不再继续接收事件.
    上游可以不发送onComplete或onError.
    最为关键的是onComplete和onError必须唯一并且互斥, 即不能发多个onComplete, 也不能发多个onError, 也不能先发一个onComplete, 然后再发一个onError, 反之亦然

注: 关于onComplete和onError唯一并且互斥这一点, 是需要自行在代码中进行控制, 如果你的代码逻辑中违背了这个规则, **并不一定会导致程序崩溃. ** 比如发送多个onComplete是可以正常运行的, 依然是收到第一个onComplete就不再接收了, 但若是发送多个onError, 则收到第二个onError事件会导致程序会崩溃.
 重载方法
 public final Disposable subscribe() {} public final Disposable subscribe(Consumer<? super T> onNext) {}
 public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {} 
 public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {} 
 public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete, Consumer<? super Disposable> onSubscribe) {} 
 public final void subscribe(Observer<? super T> observer) {}

	最后一个带有Observer参数的我们已经使用过了,这里对其他几个方法进行说明.
    不带任何参数的subscribe() 表示下游不关心任何事件,你上游尽管发你的数据去吧, 老子可不管你发什么.
    带有一个Consumer参数的方法表示下游只关心onNext事件, 其他的事件我假装没看见, 因此我们如果只需要onNext事件可以这么写:
	Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
                Log.d(TAG, "emit 2");
                emitter.onNext(2);
                Log.d(TAG, "emit 3");
                emitter.onNext(3);
                Log.d(TAG, "emit complete");
                emitter.onComplete();
                Log.d(TAG, "emit 4");
                emitter.onNext(4);
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "onNext: " + integer);
            }
        });

介绍了ObservableEmitter, 接下来介绍Disposable, 这个单词的字面意思是一次性用品,用完即可丢弃的. 那么在RxJava中怎么去理解它呢, 对应于上面的水管的例子, 我们可以把它理解成两根管道之间的一个机关, 当调用它的dispose()方法时, 它就会将两根管道切断, 从而导致下游收不到事件.
注意: 调用dispose()并不会导致上游不再继续发送事件, 上游会继续发送剩余的事件.


# 教程二
## 学习RxJava强大的线程控制
以Android为例, 一个Activity的所有动作默认都是在主线程中运行的, 比如我们在onCreate中打出当前线程的名字:

	 @Override 
	protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setContentView(R.layout.activity_main); Log.d(TAG, Thread.currentThread().getName()); }
	结果：D/TAG: main

回到RxJava中, 当我们在主线程中去创建一个上游Observable来发送事件, 则这个上游默认就在主线程发送事件.

当我们在主线程去创建一个下游Observer来接收事件, 则这个下游默认就在主线程中接收事件, 来看段代码:回到RxJava中, 当我们在主线程中去创建一个上游Observable来发送事件, 则这个上游默认就在主线程发送事件.

当我们在主线程去创建一个下游Observer来接收事件, 则这个下游默认就在主线程中接收事件, 来看段代码:

	 @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                    Log.d(TAG, "Observable thread is : " + Thread.currentThread().getName());
                    Log.d(TAG, "emit 1");
                    emitter.onNext(1);
                }
            });
            Consumer<Integer> consumer = new Consumer<Integer>() {
                @Override
                public void accept(Integer integer) throws Exception {
                    Log.d(TAG, "Observer thread is :" + Thread.currentThread().getName());
                    Log.d(TAG, "onNext: " + integer);
                }
            };
            observable.subscribe(consumer);
        }
	结果：D/TAG: Observable thread is : main
		D/TAG: emit 1                     
		D/TAG: Observer thread is :main   
		D/TAG: onNext: 1  



 @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "Observable thread is : " + Thread.currentThread().getName());
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
            }
        });
        Consumer<Integer> consumer = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "Observer thread is :" + Thread.currentThread().getName());
                Log.d(TAG, "onNext: " + integer);
            }
        };
        observable.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);
    }
	结果： D/TAG: Observable thread is : RxNewThreadScheduler-2  
 			D/TAG: emit 1                                         
			 D/TAG: Observer thread is :main                       
 			D/TAG: onNext: 1
 	在RxJava中, 已经内置了很多线程选项供我们选择, 例如有

        Schedulers.io() 代表io操作的线程, 通常用于网络,读写文件等io密集型的操作
        Schedulers.computation() 代表CPU计算密集型的操作, 例如需要大量计算的操作
        Schedulers.newThread() 代表一个常规的新线程
        AndroidSchedulers.mainThread() 代表Android的主线程
##网络请求
	1.要使用Retrofit,先添加Gradle配置:
		 //retrofit compile 'com.squareup.retrofit2:retrofit:2.1.0' //Gson converter compile 'com.squareup.retrofit2:converter-gson:2.1.0' //RxJava2 Adapter compile 'com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0' //okhttp compile 'com.squareup.okhttp3:okhttp:3.4.1' compile 'com.squareup.okhttp3:logging-interceptor:3.4.1'
	2.随后定义Api接口:
		public interface Api { @GET Observable<LoginResponse> login(@Body LoginRequest request); @GET Observable<RegisterResponse> register(@Body RegisterRequest request); }
	3.接着创建一个Retrofit客户端:
		 private static Retrofit create() {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.connectTimeout(9, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }
        return new Retrofit.Builder().baseUrl(ENDPOINT).client(builder.build()).addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build();
    }
	4.发起请求
	  Api api = retrofit.create(Api.class); api.login(request).
    subscribeOn(Schedulers.io()) //在IO线程进行网络请求 .
    observeOn(AndroidSchedulers.mainThread()) //回到主线程去处理请求结果 
    .subscribe(new Observer<LoginResponse>() {
        @Override public void onSubscribe (Disposable d){
        }
        @Override public void onNext (LoginResponse value){
        }
        @Override public void onError (Throwable e){
            Toast.makeText(mContext, "登录失败", Toast.LENGTH_SHORT).show();
        }
        @Override public void onComplete () {
            Toast.makeText(mContext, "登录成功", Toast.LENGTH_SHORT).show();
        }
    });

	看似很完美, 但我们忽略了一点, 如果在请求的过程中Activity已经退出了, 这个时候如果回到主线程去更新UI, 那么APP肯定就崩溃了, 怎么办呢, 上一节我们说到了Disposable , 说它是个开关, 调用它的dispose()方法时就会切断水管, 使得下游收不到事件, 既然收不到事件, 那么也就不会再去更新UI了. 因此我们可以在Activity中将这个Disposable 保存起来, 当Activity退出时, 切断它即可.
	
	那如果有多个Disposable 该怎么办呢, RxJava中已经内置了一个容器CompositeDisposable, 每当我们得到一个Disposable时就调用CompositeDisposable.add()将它添加到容器中, 在退出的时候, 调用CompositeDisposable.clear() 即可切断所有的水管.

#教程三
## Map
	map是RxJava中最简单的一个变换操作符了, 它的作用就是对上游发送的每一个事件应用一个函数, 使得每一个事件都按照指定的函数去变化. 用事件图表示如下:
	Observable.create(new ObservableOnSubscribe<Integer>(){
        @Override public void subscribe (ObservableEmitter < Integer > emitter) throws Exception {
        emitter.onNext(1);
        emitter.onNext(2);
        emitter.onNext(3);
    }
    }).map(new Function<Integer, String>() {
        @Override public String apply (Integer integer) throws Exception {
            return "This is result " + integer;
        }
    }).subscribe(new Consumer<String>() {
        @Override public void accept (String s) throws Exception {
            Log.d(TAG, s);
        }
    });
	D/TAG: This is result 1 
 	D/TAG: This is result 2 
	D/TAG: This is result 3 
##FlatMap
	flatMap是一个非常强大的操作符, 先用一个比较难懂的概念说明一下:
	FlatMap将一个发送事件的上游Observable变换为多个发送事件的Observables，然后将它们发射的事件合并后放进一个单独的Observable里.
	Observable.create(new ObservableOnSubscribe<Integer>(){
        @Override public void subscribe (ObservableEmitter < Integer > emitter) throws Exception {
        emitter.onNext(1);
        emitter.onNext(2);
        emitter.onNext(3);
    }
    }).flatMap(new Function<Integer, ObservableSource<String>>() {
        @Override public ObservableSource<String> apply (Integer integer) throws Exception {
            final List<String> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add("I am value " + integer);
            }
            return Observable.fromIterable(list).delay(10, TimeUnit.MILLISECONDS);
        }
    }).subscribe(new Consumer<String>() {
        @Override public void accept (String s) throws Exception {
            Log.d(TAG, s);
        }
    });
	D/TAG: I am value 1 
	D/TAG: I am value 1 
	D/TAG: I am value 1 
	D/TAG: I am value 3 
	D/TAG: I am value 3 
	D/TAG: I am value 3 
	D/TAG: I am value 2 
	D/TAG: I am value 2 
	D/TAG: I am value 2
这里也简单说一下concatMap吧, 它和flatMap的作用几乎一模一样, 只是它的结果是严格按照上游发送的顺序来发送的
#教程四
##Zip的使用

	Zip通过一个函数将多个Observable发送的事件结合到一起，然后发送这些组合到一起的事件. 它按照严格的顺序应用这个函数。它只发射与发射数据项最少的那个Observable一样多的数据。

 Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
                Log.d(TAG, "emit 2");
                emitter.onNext(2);
                Log.d(TAG, "emit 3");
                emitter.onNext(3);
                Log.d(TAG, "emit 4");
                emitter.onNext(4);
                Log.d(TAG, "emit complete1");
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()); ;
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "emit A");
                emitter.onNext("A");
                Log.d(TAG, "emit B");
                emitter.onNext("B");
                Log.d(TAG, "emit C");
                emitter.onNext("C");
                Log.d(TAG, "emit complete2");
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()); ;
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer + s;
            }
        }).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "onSubscribe");
            }

            @Override
            public void onNext(String value) {
                Log.d(TAG, "onNext: " + value);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete");
            }
        });
		D/TAG: onSubscribe 
		D/TAG: emit A 
		D/TAG: emit 1 
		D/TAG: onNext: 1A 
		D/TAG: emit B 
		D/TAG: emit 2 
		D/TAG: onNext: 2B
		 D/TAG: emit C 
		D/TAG: emit 3 
		D/TAG: onNext: 3C 
		D/TAG: emit complete2 
		D/TAG: onComplete
	.subscribeOn(Schedulers.io())让两个时间运行到不同线程，结果就会依次发送
	这下就对了嘛, 两根水管同时开始发送, 每发送一个, Zip就组合一个, 再将组合结果发送给下游.
	这是因为我们之前说了, zip发送的事件数量跟上游中发送事件最少的那一根水管的事件数量是有关的, 在这个例子里我们第二根水管只发送了三个事件然后就发送了Complete, 这个时候尽管第一根水管还有事件4 和事件Complete 没有发送, 但是它们发不发送还有什么意义呢? 所以本着节约是美德的思想, 就干脆打断它的狗腿, 不让它发了.


#教程五
##大家喜闻乐见的Backpressure来啦.
	如图中所示, 其中蓝色的框框就是zip给我们的水缸! 它将每根水管发出的事件保存起来, 等两个水缸都有事件了之后就分别从水缸中取出一个事件来组合, 当其中一个水缸是空的时候就处于等待的状态.

	题外话: 大家来分析一下这个水缸有什么特点呢? 它是按顺序保存的, 先进来的事件先取出来, 这个特点是不是很熟悉呀? 没错, 这就是我们熟知的队列, 这个水缸在Zip内部的实现就是用的队列, 感兴趣的可以翻看源码查看.
	
	好了回到正题上来, 这个水缸有大小限制吗? 要是一直往里存会怎样? 我们来看个例子:
	Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
                    //无限循环发事件
                    emitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io());
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onNext("A");
            }
        }).subscribeOn(Schedulers.io());
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer + s;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.d(TAG, s);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.w(TAG, throwable);
            }
        });
	最终OOM了，出现这种情况肯定是我们不想看见的, 这里就可以引出我们的Backpressure了, 所谓的Backpressure其实就是为了控制流量, 水缸存储的能力毕竟有限, 因此我们还得从源头去解决问题, 既然你发那么快, 数据量那么大, 那我就想办法不让你发那么快呗.
	
	//内存未发生明显变化，由于在同一线程处理事件，没有中间水缸
	 private void case08() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) { //无限循环发事件
                    emitter.onNext(i);
                }
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Thread.sleep(2000);
                Log.d(TAG, "" + integer);
            }
        });

    }

    //内存发生明显变化，由于在在不同线程处理事件，上游发送的事件下游不能及时处理，需要容器，因此会导致OOM
        private void case09() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) { //无限循环发事件
                    emitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Thread.sleep(2000);
                Log.d(TAG, "" + integer);
            }
        });

    }

   为什么不加线程和加上线程区别这么大呢, 这就涉及了同步和异步的知识了.

	当上下游工作在同一个线程中时, 这时候是一个同步的订阅关系, 也就是说上游每发送一个事件必须等到下游接收处理完了以后才能接着发送下一个事件.
	
	当上下游工作在不同的线程中时, 这时候是一个异步的订阅关系, 这个时候上游发送数据不需要等待下游接收, 为什么呢, 因为两个线程并不能直接进行通信, 因此上游发送的事件并不能直接到下游里去, 这个时候就需要一个田螺姑娘来帮助它们俩, 这个田螺姑娘就是我们刚才说的水缸 ! 上游把事件发送到水缸里去, 下游从水缸里取出事件来处理, 因此, 当上游发事件的速度太快, 下游取事件的速度太慢, 水缸就会迅速装满, 然后溢出来, 最后就OOM了.
	我们可以看出, 同步和异步的区别仅仅在于是否有水缸.
#教程六

	用了一个sample操作符, 简单做个介绍, 这个操作符每隔指定的时间就从上游中取出一个事件发送给下游. 这里我们让它每隔2秒取一个事件给下游
	private void case10() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
                    emitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io()).sample(2, TimeUnit.SECONDS)
                //sample取样
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "" + integer);
            }
        });

    }
	我们让上游每次发送完事件后都延时了2秒
	  private void case11() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
                    emitter.onNext(i);
                    Thread.sleep(2000);
                    //每次发送完事件延时2秒
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "" + integer);
            }
        });

    }

	private void case12() {
        Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
                    emitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io()).sample(2, TimeUnit.SECONDS);
        //进行sample采样
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onNext("A");
            }
        }).subscribeOn(Schedulers.io());
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer + s;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.d(TAG, s);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.w(TAG, throwable);
            }
        });
    }

	 private void case13() {
        Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
                    emitter.onNext(i);
                    Thread.sleep(2000);
                    //发送事件之后延时2秒
                }
            }
        }).subscribeOn(Schedulers.io());
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onNext("A");
            }
        }).subscribeOn(Schedulers.io());
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer + s;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.d(TAG, s);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.w(TAG, throwable);
            }
        });
    }


	因此我们总结一下, 本节中的治理的办法就两种:
    一是从数量上进行治理, 减少发送进水缸里的事件
    二是从速度上进行治理, 减缓事件发送进水缸的速度
通过本节的学习, 大家应该对如何处理上下游流速不均衡已经有了基本的认识了, 大家也可以看到, 我们并没有使用Flowable, 所以很多时候仔细去分析问题, 找到问题的原因, 从源头去解决才是最根本的办法. 后面我们讲到Flowable的时候, 大家就会发现它其实没什么神秘的, 它用到的办法和我们本节所讲的基本上是一样的, 只是它稍微做了点封装.



    
	

















