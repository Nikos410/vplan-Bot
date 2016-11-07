package org.nikos.vplanbot.org.nikos.vplanbot.notification;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterNotificationService implements NotificationService {

    ConfigurationBuilder cb;

    public TwitterNotificationService(){

        final String consumerKey = "NWuSWNq9rhHQ3cRFTh2u76sCn";
        final String consumerSecret = "hiXIJGqGUP0ov8uQvOpHgnLc2DMu2UTvI6vxURxlNgbzGoyrJD";
        final String accessToken = "795359608034512897-kX8nNVAp0ZVyYHVra5MCyHCbTYBhpP5";
        final String accessSecret = "Xk8A8p8Bgz6QloDOqYYGGyyWcW0TDICqe8YvsiKp4Fuce";

        cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessSecret);
    }

    public void sendNotification(String tweetText){

        try
        {
            TwitterFactory factory = new TwitterFactory(cb.build());
            Twitter twitter = factory.getInstance();

            Status status = twitter.updateStatus(tweetText);

        }catch (TwitterException te) {
            te.printStackTrace();
            System.exit(-1);
        }

    }
}
