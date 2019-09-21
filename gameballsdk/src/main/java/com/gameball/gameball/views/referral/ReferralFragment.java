package com.gameball.gameball.views.referral;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Game;
import com.gameball.gameball.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.Locale;

public class ReferralFragment extends Fragment implements ReferralContract.View, View.OnClickListener {
    private View rootView;
    private ClientBotSettings clientBotSettings;

    private TextView shareLinkBtn;
    private TextView referralLink;
    private RecyclerView referralChallengeLs;
    private TextView referralHeadline;
    private TextView referralText;

    private ReferralChallengesAdapter referralChallengesAdapter;
    ReferralContract.Presenter presenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_referral, container, false);
        initView();
        setupBotSettings();
        prepView();
        presenter.getReferralChallenges();
        referralLink.setText(SharedPreferencesUtils.getInstance().getPlayerReferralLink());
        return rootView;
    }

    private void initComponents()
    {
        presenter = new ReferralPresenter(this);
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        referralChallengesAdapter = new ReferralChallengesAdapter(getContext(),new ArrayList<Game>());
    }

    private void setupBotSettings()
    {
        referralHeadline.setTextColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        shareLinkBtn.setBackgroundTintList(ColorStateList
                .valueOf(Color.parseColor(clientBotSettings.getBotMainColor())));

    }

    private void initView()
    {
        shareLinkBtn = rootView.findViewById(R.id.share_link_btn);
        referralLink = rootView.findViewById(R.id.referral_link);
        referralHeadline = rootView.findViewById(R.id.referral_headline);
        referralText = rootView.findViewById(R.id.referral_text);
        referralChallengeLs = rootView.findViewById(R.id.referral_challenge_ls);

        referralChallengeLs.setLayoutManager(new LinearLayoutManager(getContext()));
        referralChallengeLs.setNestedScrollingEnabled(true);
        referralChallengeLs.setHasFixedSize(true);
        referralChallengeLs.setAdapter(referralChallengesAdapter);

        if(DisplayUtils.isRTL(Locale.getDefault()))
        {
            shareLinkBtn.setBackgroundResource(R.drawable.bg_primary_left_corener_round);
            referralLink.setBackgroundResource(R.drawable.bg_grey_right_corener_round);
        }

        shareLinkBtn.setOnClickListener(this);
    }

    private void prepView() {
        referralHeadline.setText(clientBotSettings.getReferralHeadLine());
        referralText.setText(clientBotSettings.getReferralText());
    }

    @Override
    public void onReferralChallengesFiltered(ArrayList<Game> games)
    {
        referralChallengesAdapter.setmData(games);
        referralChallengesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        if(referralLink.getText().toString().isEmpty())
            return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, referralLink.getText().toString());
        startActivity(Intent.createChooser(shareIntent, "Share link using"));


    }
}
