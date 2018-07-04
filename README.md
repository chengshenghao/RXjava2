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





