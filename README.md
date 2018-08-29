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














