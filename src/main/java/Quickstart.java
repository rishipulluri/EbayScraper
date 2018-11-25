import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.*;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;

import java.sql.*;


public class Quickstart {

    /** Application name. */
    private static final String APPLICATION_NAME = "API Sample";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/youtube-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    static final List<String> categories = new ArrayList<String>() {{
        add("electronics");
        add("phones");
        add("memes");
        add("trump");
        add("sports");
        add("messi");
        add("toys");
        add("tools");
        add("watches");
        add("vacuums");
    }};


    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final Collection<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/youtube.force-ssl");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Create an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
            Quickstart.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
         LocalServerReceiver localServerReceiver = new LocalServerReceiver.Builder().setHost("localhost").setPort(8181).build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, localServerReceiver).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized API client service, such as a YouTube
     * Data API client service.
     * @return an authorized API client service
     * @throws IOException
     */
    public static YouTube getYouTubeService() throws IOException {
        Credential credential = authorize();
        return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("com.mysql.jdbc.Driver");
        Random rand = new Random();
        int cat_number = rand.nextInt(10); 
        String cat = categories.get(cat_number);
        YouTube youtube = getYouTubeService();
        try {
            //database stuff
            String connectionUrl = "jdbc:mysql://localhost:3306/ebay_data";
            Connection conn = DriverManager.getConnection(connectionUrl, "root", "Shreyas123");
            ResultSet rs = conn.prepareStatement("show tables").executeQuery();
 
            //get all tables in database
            while(rs.next()){
                String s = rs.getString(1);
                System.out.println(s);
            }


            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet");
            parameters.put("maxResults", "20");
            parameters.put("q", cat);
            parameters.put("type", "");
            parameters.put("chart", "mostPopular");

            YouTube.Search.List searchListByKeywordRequest = youtube.search().list(parameters.get("part").toString());
            if (parameters.containsKey("maxResults")) {
                searchListByKeywordRequest.setMaxResults(Long.parseLong(parameters.get("maxResults").toString()));
            }

            if (parameters.containsKey("q") && parameters.get("q") != "") {
                searchListByKeywordRequest.setQ(parameters.get("q").toString());
            }

            if (parameters.containsKey("type") && parameters.get("type") != "") {
                searchListByKeywordRequest.setType(parameters.get("type").toString());
            }
            SearchListResponse response = searchListByKeywordRequest.execute();
            List<Videos> videos = parseYoutubeVideos(response.toString());
            List<EbayItem> ebayItems = getEbayItems(cat);
            postComments(ebayItems, videos, youtube, conn);          
            System.out.flush();
            YouTube.Channels.List channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics");
            channelsListByUsernameRequest.setForUsername("GoogleDevelopers");

            ChannelListResponse searchResponse = channelsListByUsernameRequest.execute();
            Channel channel = searchResponse.getItems().get(0);
            System.out.printf(
                "This channel's ID is %s. Its title is '%s', and it has %s views.\n",
                channel.getId(),
                channel.getSnippet().getTitle(),
                channel.getStatistics().getViewCount());
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " +
                e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static List<Videos> parseYoutubeVideos(String jsonText) throws Exception  {
        JSONObject object = new JSONObject(jsonText);
        JSONArray arr1 = object.getJSONArray("items");
        List<Videos> videos = new ArrayList<>();
        for (int i = 0; i < arr1.length(); i++) {
            try {
                Videos vids = new Videos();
                JSONObject video = arr1.getJSONObject(i);
                String videoID = video.getJSONObject("id").getString("videoId");
                String channelID = video.getJSONObject("snippet").getString("channelId");
                String title = video.getJSONObject("snippet").getString("title");
                vids.setTitle(title);
                vids.setVideoID(videoID);
                vids.setChannelID(channelID);
                videos.add(vids);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
        }
        return videos;
    }

    private static List<EbayItem> getEbayItems(String searchterm) throws Exception {
        String campaignID = "5338433291";
        String url = "http://svcs.ebay.com/services/search/FindingService/v1";
        url += "?OPERATION-NAME=findItemsByKeywords";
        url += "&SERVICE-VERSION=1.0.0";
        url += "&SECURITY-APPNAME=rishipul-SellerLi-PRD-0c272c1de-10fe2786";
        url += "&GLOBAL-ID=EBAY-US";
        url += "&RESPONSE-DATA-FORMAT=JSON";
        url += "&REST-PAYLOAD";
        url += "&keywords=" + searchterm;
        url += "&paginationInput.entriesPerPage=20";
        url += "&affiliate.trackingId=" + campaignID;
        url += "&affiliate.networkId=9";
         
        List<EbayItem> items = new ArrayList<>();
         
         URL obj = new URL(url);
         HttpURLConnection con = (HttpURLConnection) obj.openConnection();
         // optional default is GET
         con.setRequestMethod("GET");
         //add request header
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         int responseCode = con.getResponseCode();
         System.out.println("\nSending 'GET' request to URL : " + url);
         System.out.println("Response Code : " + responseCode);
         BufferedReader in = new BufferedReader(
                 new InputStreamReader(con.getInputStream()));
         String inputLine;
         StringBuffer response = new StringBuffer();
         while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
         }
         in.close();
         //Read JSON response and print
         JSONObject myresponse = new JSONObject(response.toString());
         JSONArray arr1 =myresponse.getJSONArray("findItemsByKeywordsResponse");
         for (int i = 0; i < arr1.length(); i++) {
             JSONObject obj2 = arr1.getJSONObject(i);
             JSONArray arr2 = obj2.getJSONArray("searchResult");
             for (int j = 0; j < arr2.length(); j++) {
                 JSONArray arr3 = arr2.getJSONObject(i).getJSONArray("item");
                 for (int k = 0; k < arr3.length(); k++) {
                     JSONObject obj4 = arr3.getJSONObject(k);
                     EbayItem item = new EbayItem();
                     JSONArray affURLs = obj4.getJSONArray("viewItemURL");
                     JSONArray titles = obj4.getJSONArray("title");
                     JSONArray galleryURLs = obj4.getJSONArray("galleryURL");
                     JSONArray sellingStatus = obj4.getJSONArray("sellingStatus");
                     for (int b = 0; b < 1; b++) {
                         String affURL = affURLs.getString(b);
                         item.setItemURL(affURL);
                     }
                     for (int b = 0; b < 1; b++) {
                         String title = titles.getString(b);
                         item.setName(title);
                     }
                     for (int b = 0; b < 1; b++) {
                         String galleryURL = galleryURLs.getString(b);
                         item.setImageURL(galleryURL);
                     }
                     for (int b = 0; b < 1; b++) {
                         JSONArray convertedCurrentPrice = sellingStatus.getJSONObject(b).getJSONArray("convertedCurrentPrice");
                         for (int c = 0; c < 1; c++) {
                             JSONObject prices = convertedCurrentPrice.getJSONObject(c);
                             String currency = prices.getString("@currencyId");
                             item.setCurrency(currency);
                             String price = prices.getString("__value__");
                             item.setPrice(price);
                         } 
                     }
                     items.add(item);
                 }
             }
         }
         return items;
    }

    private static void postComments(List<EbayItem> items, List<Videos> videos, YouTube youtube, Connection conn)
        throws IOException, ClassNotFoundException, SQLException {
        for (int i = 0; i < items.size(); i++) {
            EbayItem ebayItem = items.get(i);
            Videos video = videos.get(i);
            try {
                //insert into mysql database
                Statement mystmt = conn.createStatement();
                String title = video.getTitle().replaceAll("'", "");
                String itemURL = ebayItem.getItemURL().replaceAll("'", "");
                String price = ebayItem.getPrice().replaceAll("'", "");
                String imageURL = ebayItem.getImageURL().replaceAll("'", "");
                String name = ebayItem.getName().replaceAll("'", "");
                String insertSql = "insert into youtube_ebay_data " + " (video_title, ebay_product_link, price, item_image, product_title) " + "values ('"
                                + title + "', '" + itemURL + "', '" + price + "', '" + imageURL + "', '"
                                + name + "')";
                System.out.println(insertSql);
                mystmt.execute(insertSql);
                String formatText = "Come get the new " + ebayItem.getName() + " right now." + " Click on this link for more details " 
                + ebayItem.getItemURL() + ".";

                // Insert channel comment by omitting videoId.
                // Create a comment snippet with text.
                CommentSnippet commentSnippet = new CommentSnippet();
                commentSnippet.setTextOriginal(formatText);

                // Create a top-level comment with snippet.
                Comment topLevelComment = new Comment();
                topLevelComment.setSnippet(commentSnippet);

                // Create a comment thread snippet with channelId and top-level
                // comment.
                CommentThreadSnippet commentThreadSnippet = new CommentThreadSnippet();
                commentThreadSnippet.setChannelId(video.getChannelID());
                commentThreadSnippet.setTopLevelComment(topLevelComment);

                // Create a comment thread with snippet.
                CommentThread commentThread = new CommentThread();
                commentThread.setSnippet(commentThreadSnippet);

                // Call the YouTube Data API's commentThreads.insert method to
                // create a comment.
                CommentThread channelCommentInsertResponse = youtube.commentThreads()
                        .insert("snippet", commentThread).execute();
                // Print information from the API response.
                System.out
                        .println("\n================== Created Channel Comment ==================\n");
                CommentSnippet snippet = channelCommentInsertResponse.getSnippet().getTopLevelComment()
                        .getSnippet();
                System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                System.out.println("  - Comment: " + snippet.getTextDisplay());
                System.out
                        .println("\n-------------------------------------------------------------\n");

                // Insert video comment
                commentThreadSnippet.setVideoId(video.getVideoID());
                // Call the YouTube Data API's commentThreads.insert method to
                // create a comment.
                CommentThread videoCommentInsertResponse = youtube.commentThreads()
                        .insert("snippet", commentThread).execute();
                // Print information from the API response.
                System.out
                        .println("\n================== Created Video Comment ==================\n");
                snippet = videoCommentInsertResponse.getSnippet().getTopLevelComment()
                        .getSnippet();
                System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                System.out.println("  - Comment: " + snippet.getTextDisplay());
                System.out.println("  - video title: " + video.getTitle());
                System.out
                        .println("\n-------------------------------------------------------------\n");
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
                continue;
            }

        }
    }
}

