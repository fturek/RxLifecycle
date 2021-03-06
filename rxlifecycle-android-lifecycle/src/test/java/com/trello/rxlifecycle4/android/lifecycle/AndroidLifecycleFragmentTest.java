package com.trello.rxlifecycle4.android.lifecycle;

import com.trello.lifecycle4.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle4.LifecycleProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AndroidLifecycleFragmentTest {
    private Observable<Object> observable;

    @Before
    public void setup() {
        observable = PublishSubject.create().hide();
    }

    @Test
    public void testLifecycleFragment() {
        testLifecycle(new Fragment());
        testBindUntilEvent(new Fragment());
        testBindToLifecycle(new Fragment());
    }

    private void testLifecycle(LifecycleOwner owner) {
        Fragment fragment = (Fragment) owner;
        ActivityController<?> controller = startFragment(fragment);

        TestObserver<Lifecycle.Event> testObserver = AndroidLifecycle.createLifecycleProvider(owner).lifecycle().test();

        controller.start();
        controller.resume();
        controller.pause();
        controller.stop();
        controller.destroy();

        testObserver.assertValues(
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY
        );
    }

    // Tests bindUntil for any given AndroidLifecycle Fragment implementation
    private void testBindUntilEvent(LifecycleOwner owner) {
        Fragment fragment = (Fragment) owner;
        ActivityController<?> controller = startFragment(fragment);

        TestObserver<Object> testObserver = observable.compose(AndroidLifecycle.createLifecycleProvider(owner).bindUntilEvent(Lifecycle.Event.ON_STOP)).test();

        testObserver.assertNotComplete();
        controller.start();
        testObserver.assertNotComplete();
        controller.resume();
        testObserver.assertNotComplete();
        controller.pause();
        testObserver.assertNotComplete();
        controller.stop();
        testObserver.assertComplete();
    }

    // Tests bindToLifecycle for any given RxLifecycle Fragment implementation
    private void testBindToLifecycle(LifecycleOwner owner) {
        Fragment fragment = (Fragment) owner;
        LifecycleProvider<Lifecycle.Event> provider = AndroidLifecycle.createLifecycleProvider(owner);
        ActivityController<?> controller = startFragment(fragment);

        TestObserver<Object> createObserver = observable.compose(provider.bindToLifecycle()).test();

        controller.start();
        createObserver.assertNotComplete();
        TestObserver<Object> startObserver = observable.compose(provider.bindToLifecycle()).test();

        controller.resume();
        createObserver.assertNotComplete();
        startObserver.assertNotComplete();
        TestObserver<Object> resumeObserver = observable.compose(provider.bindToLifecycle()).test();

        controller.pause();
        createObserver.assertNotComplete();
        startObserver.assertNotComplete();
        resumeObserver.assertComplete();
        TestObserver<Object> pauseObserver = observable.compose(provider.bindToLifecycle()).test();

        controller.stop();
        createObserver.assertNotComplete();
        startObserver.assertComplete();
        pauseObserver.assertComplete();
        TestObserver<Object> stopObserver = observable.compose(provider.bindToLifecycle()).test();

        controller.destroy();
        createObserver.assertComplete();
        stopObserver.assertComplete();
    }

    // Easier than making everyone create their own shadows
    private ActivityController<FragmentActivity> startFragment(Fragment fragment) {
        ActivityController<FragmentActivity> controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.create();
        controller.get().getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        return controller;
    }
}
