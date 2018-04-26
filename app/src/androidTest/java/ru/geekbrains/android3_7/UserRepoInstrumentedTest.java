package ru.geekbrains.android3_7;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import ru.geekbrains.android3_7.di.DaggerTestComponent;
import ru.geekbrains.android3_7.di.TestComponent;
import ru.geekbrains.android3_7.di.modules.ApiModule;
import ru.geekbrains.android3_7.model.cache.ICache;
import ru.geekbrains.android3_7.model.cache.IImageCache;
import ru.geekbrains.android3_7.model.cache.RealmImageCache;
import ru.geekbrains.android3_7.model.entity.Repository;
import ru.geekbrains.android3_7.model.entity.User;
import ru.geekbrains.android3_7.model.repo.UsersRepo;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UserRepoInstrumentedTest
{
    private static MockWebServer webServer;

    @Inject
    UsersRepo usersRepo;

    @Inject
    ICache iCache;


    @Test
    public void useAppContext() throws Exception
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("ru.geekbrains.android3_7", appContext.getPackageName());
    }

    @BeforeClass
    public static void setupClass() throws IOException
    {
        webServer = new MockWebServer();
        webServer.start();
    }

    @AfterClass
    public static void tearDownClass() throws IOException
    {
        webServer.shutdown();
    }

    @Before
    public void setup()
    {
        TestComponent component = DaggerTestComponent
                .builder()
                .apiModule(new ApiModule(){
                    @Override
                    public String endpoint()
                    {
                        return webServer.url("/").toString();
                    }
                })
                .build();

        component.inject(this);
    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void getUser()
    {
        webServer.enqueue(createUserResponse("somelogin", "someurl"));
        TestObserver<User> observer = new TestObserver<>();
        usersRepo.getUser("somelogin").subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(1);
        assertEquals(observer.values().get(0).getLogin(), "somelogin");
        assertEquals(observer.values().get(0).getAvatarUrl(), "someurl");
    }

    private MockResponse createUserResponse(String login, String avatar)
    {
        String body = "{\"login\":\"" + login + "\", \"avatar_url\":\"" + avatar +"\"}";
        return new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(body);
    }

    @Test
    public void putUser(){
        String login = "kasha";
        String avatarUrl = "malasha";
        User user = new User(login,avatarUrl);
        iCache.putUser(user);

        TestObserver<User> observer = new TestObserver<>();
        iCache.getUser(login).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(1);
        assertEquals(observer.values().get(0).getLogin(), login);
        assertEquals(observer.values().get(0).getAvatarUrl(), avatarUrl);
    }

    @Test
    public void putUserRepos(){

        putUserReposNewUser();
        putUserReposOldUser();

    }

    public void putUserReposNewUser(){
        String login = "kasha";
        String avatarUrl = "malasha";
        User user = new User(login,avatarUrl);
       // iCache.putUser(user);

        String id = "1";
        String name = "test";
        ArrayList<Repository> repos = new ArrayList<>();
        repos.add(new Repository(id,name));
        iCache.putUserRepos(user,repos);

        TestObserver<List<Repository>> observer = new TestObserver<>();
        iCache.getUserRepos(user).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(1);
        assertEquals(observer.values().get(0).get(0).getId(), id);
        assertEquals(observer.values().get(0).get(0).getName(), name);
    }

    public void putUserReposOldUser(){
        String login = "kasha";
        String avatarUrl = "malasha";
        User user = new User(login,avatarUrl);
        iCache.putUser(user);

        String id = "1";
        String name = "test";
        ArrayList<Repository> repos = new ArrayList<>();
        repos.add(new Repository(id,name));
        iCache.putUserRepos(user,repos);

        TestObserver<List<Repository>> observer = new TestObserver<>();
        iCache.getUserRepos(user).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(1);
        assertEquals(observer.values().get(0).get(0).getId(), id);
        assertEquals(observer.values().get(0).get(0).getName(), name);
    }


    @Test
    public void getUserFromCache(){
        String login = "kasha2";
        String avatarUrl = "malasha2";
        User user = new User(login,avatarUrl);

        TestObserver<User> observer = new TestObserver<>();
        iCache.getUser(login).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(0);
    }


    @Test
    public void getUserReposFromCache(){

        getUserReposNewUser();
        getUserReposExistUser();

    }


    public void getUserReposNewUser(){
        String login = "kasha111";
        String avatarUrl = "malasha";
        User user = new User(login,avatarUrl);

        TestObserver<List<Repository>> observer = new TestObserver<>();
        iCache.getUserRepos(user).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(0);
    }


    public void getUserReposExistUser(){
        String login = "kasha12";
        String avatarUrl = "malasha";
        User user = new User(login,avatarUrl);

        String id = "2";
        String name = "test";
        ArrayList<Repository> repos = new ArrayList<>();
        repos.add(new Repository(id,name));
        iCache.putUserRepos(user,repos);

        TestObserver<List<Repository>> observer = new TestObserver<>();
        iCache.getUserRepos(user).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(1);
    }




    @Test
    public void saveImage() throws InterruptedException {

        int[] colors = new int[300*300];
        Arrays.fill(colors, 0, 300*100, Color.argb(85, 255, 0, 0));
        Arrays.fill(colors, 300*100, 300*200, Color.GREEN);
        Arrays.fill(colors, 300*200, 300*300, Color.BLUE);

        Bitmap bitmap = Bitmap.createBitmap(colors, 300, 300, Bitmap.Config.RGB_565);
        IImageCache iImageCache = new RealmImageCache();

        String url = "test9.jpg";
        File file = iImageCache.saveImage(url, bitmap);

        //TimeUnit.SECONDS.sleep(10);

        File file2 = iImageCache.getFile(url);

        assertEquals(file,file2);

    }

}
