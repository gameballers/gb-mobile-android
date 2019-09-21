package com.gameball.gameball.views.notification;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Notification;

import java.util.ArrayList;

public class NotificationFragment extends Fragment implements NotificationsContract.View {

    private View rootView;

    private TextView notificationsTitle;
    private RecyclerView notificationsList;
    private RelativeLayout noInternetLayout;

    private ProgressBar loadingIndicator;
    private NotificationsHistoryAdapter adapter;
    private NotificationsContract.Presenter presenter;
    private ClientBotSettings clientBotSettings;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new NotificationsHistoryAdapter(getContext(), new ArrayList<Notification>());
        presenter = new NotificationsPresenter(this);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_notification, container, false);
        initView();
        setupBotSettings();
        presenter.getNotificationHistory();
        return rootView;
    }

    private void initView()
    {
        notificationsTitle = rootView.findViewById(R.id.notification_title);
        notificationsList = rootView.findViewById(R.id.notification_list);
        loadingIndicator = rootView.findViewById(R.id.loading_indicator);
        noInternetLayout = rootView.findViewById(R.id.no_internet_layout);

        notificationsList.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsList.setHasFixedSize(true);
        notificationsList.setAdapter(adapter);
    }

    private void setupBotSettings() {
        loadingIndicator.getIndeterminateDrawable().setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()),
                PorterDuff.Mode.SRC_IN);
        notificationsTitle.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
    }

    @Override
    public void onNotificationsLoaded(ArrayList<Notification> notifications) {
        adapter.setMdata(notifications);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showNoInternetLayout() {
        noInternetLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void showLoadingIndicator() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingIndicator() {
        loadingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        presenter.unsubscribe();
        super.onStop();
    }
}
