package nikos.vplanbot.notification;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterNotificationService implements NotificationService {

    ConfigurationBuilder cb;

    public TwitterNotificationService(){

        final String consumerKey = "****";
        final String consumerSecret = "****";
        final String accessToken = "********";
        final String accessSecret = "********";

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
