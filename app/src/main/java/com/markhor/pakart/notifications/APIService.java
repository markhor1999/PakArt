package com.markhor.pakart.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {"Content-Type:application/json",
            "Authorization:key=AAAAll0QCSY:APA91bHpw-mZ02bfjJqEK3wKs83JxOi896w7kv1cUpvdBTLKJdpAKwrGFBDwSdlixWZ1Bkl-CLYHYxn2jg3qqv_3GFLaKFQv0hwWNHLB1QCrcvGWgdRGORWXKxc4q-HgJKVdVwcma0wb"})
    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
