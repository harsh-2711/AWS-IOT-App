package com.example.harshpatel.aws_amplify_iot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.Response;
import com.amazonaws.amplify.generated.graphql.CreateTodoMutation;
import com.amazonaws.amplify.generated.graphql.ListTodosQuery;
import com.amazonaws.amplify.generated.graphql.OnCreateTodoSubscription;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.exception.ApolloException;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import type.CreateTodoInput;


public class MainActivity extends AppCompatActivity {

    private AWSAppSyncClient mAWSAppSyncClient;

    Button send, receive, subscribe;
    EditText title, description;
    TextView responsetext;
    String responsebuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        send = (Button) findViewById(R.id.send);
        receive = (Button) findViewById(R.id.receive);
        subscribe = (Button) findViewById(R.id.subscribe);

        title = (EditText) findViewById(R.id.title) ;
        description = (EditText) findViewById(R.id.description);

        responsetext = (TextView) findViewById(R.id.responsetext);


        mAWSAppSyncClient = AWSAppSyncClient.builder()
                     .context(getApplicationContext())
                     .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                     .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                        @Override
                        public String getLatestAuthToken() {
                            try {
                                return AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                            } catch (Exception e){
                                Log.e("APPSYNC_ERROR", e.getLocalizedMessage());
                                return e.getLocalizedMessage();
                            }
                         }
                     }).build();

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String titletext = title.getText().toString();
                String descriptiontext = description.getText().toString();

                runMutation(titletext, descriptiontext);

            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runQuery();
                responsetext.setText(responsebuffer);
            }
        });

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
            }
        });

    }

    public void runMutation(String title, String description){
        CreateTodoInput createTodoInput = CreateTodoInput.builder().
                name(title).
                description(description).
                build();

        mAWSAppSyncClient.mutate(CreateTodoMutation.builder().input(createTodoInput).build())
                .enqueue(mutationCallback);
    }

    private GraphQLCall.Callback<CreateTodoMutation.Data> mutationCallback = new GraphQLCall.Callback<CreateTodoMutation.Data>() {

        @Override
        public void onResponse(@Nonnull com.apollographql.apollo.api.Response<CreateTodoMutation.Data> response) {
            Log.i("Results", "Added Todo");
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("Error", e.toString());
        }
    };

    public void runQuery(){
        mAWSAppSyncClient.query(ListTodosQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(todosCallback);
    }

    private GraphQLCall.Callback<ListTodosQuery.Data> todosCallback = new GraphQLCall.Callback<ListTodosQuery.Data>() {

        @Override
        public void onResponse(@Nonnull com.apollographql.apollo.api.Response<ListTodosQuery.Data> response) {
            Log.i("Results", response.data().listTodos().items().toString());
            responsebuffer = response.data().listTodos().items().toString();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("ERROR", e.toString());
        }
    };

    private AppSyncSubscriptionCall subscriptionWatcher;

    private void subscribe(){
        OnCreateTodoSubscription subscription = OnCreateTodoSubscription.builder().build();
        subscriptionWatcher = mAWSAppSyncClient.subscribe(subscription);
        subscriptionWatcher.execute(subCallback);
    }

    private AppSyncSubscriptionCall.Callback subCallback = new AppSyncSubscriptionCall.Callback() {

        @Override
        public void onResponse(@Nonnull com.apollographql.apollo.api.Response response) {
            Log.i("Response", response.data().toString());
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("Error", e.toString());
        }

        @Override
        public void onCompleted() {
            Log.i("Completed", "Subscription completed");
        }
    };

}
