package com.gameball.gameball.views.mainContainer;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.Level;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.DisplayUtils;
import com.gameball.gameball.utils.ImageDownloader;
import com.gameball.gameball.utils.ProgressBarAnimation;

import java.util.Locale;

public class MainContainerFragment extends DialogFragment implements MainContainerContract.View
{

    private View rootView;
    private TextView txtPlayerName;
    private ImageButton btnClose;
    private TabLayout tabs;
    private ViewPager viewPager;
    ProgressBar loadingIndicator;
    private ImageView levelLogo;
    private TextView levelName;
    private ProgressBar levelProgress;
    private TextView nextLevelTitle;
    private TextView frubiesForNextLevel;
    private TextView currentFrubiesValue;
    private TextView currentPointsValue;
    private View loadingIndicatorBg;

    private Animation fadeIn;

    TabsAdapter tabsAdapter;
    MainContainerContract.Presenter presenter;
    ClientBotSettings clientBotSettings;
    private float playerProgress;
    private TextView currentFrubiesTitle;
    private TextView currentPointTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    private void initComponents()
    {
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        tabsAdapter = new TabsAdapter(getChildFragmentManager(), clientBotSettings.isEnableLeaderboard());
        presenter = new MainContainerPresenter(this);

        fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeIn.setDuration(700);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_main_container, container, false);
        initView();
        setupBotSettings();
        presenter.getPlayerInfo();
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState)
    {

        // the content
        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // creating the fullscreen dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        return dialog;
    }

    /*@Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(width, height);
        }
    }*/

    private void initView()
    {
        txtPlayerName = rootView.findViewById(R.id.txt_player_name);
        btnClose = rootView.findViewById(R.id.btn_close);
        tabs = rootView.findViewById(R.id.tabs);
        viewPager = rootView.findViewById(R.id.view_pager);
        loadingIndicator = rootView.findViewById(R.id.loading_indicator);
        levelLogo = rootView.findViewById(R.id.level_logo);
        levelName = rootView.findViewById(R.id.level_name);
        levelProgress = rootView.findViewById(R.id.level_progress);
        nextLevelTitle = rootView.findViewById(R.id.next_level_title);
        frubiesForNextLevel = rootView.findViewById(R.id.frubies_for_next_level);
        currentFrubiesValue = rootView.findViewById(R.id.current_frubies_value);
        currentPointsValue = rootView.findViewById(R.id.current_points_value);
        currentFrubiesTitle = rootView.findViewById(R.id.frubies_title);
        currentPointTitle = rootView.findViewById(R.id.points_title);
        loadingIndicatorBg = rootView.findViewById(R.id.loading_indicator_bg);
    }

    private void setupBotSettings()
    {
        tabs.setSelectedTabIndicatorColor(Color.parseColor(clientBotSettings.getButtonBackgroundColor()));
        loadingIndicator.getIndeterminateDrawable().setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()),
                PorterDuff.Mode.SRC_IN);
        LayerDrawable progressDrawable = (LayerDrawable) levelProgress.getProgressDrawable();
        progressDrawable.setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()),
                PorterDuff.Mode.SRC_IN);

    }

    private void prepView()
    {
        viewPager.setAdapter(tabsAdapter);
        tabs.setupWithViewPager(viewPager);
        tabs.setSelectedTabIndicatorHeight((int) DisplayUtils.convertDpToPixel(2));
        btnClose.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });
        setupTabsIcons();
        tabs.getTabAt(tabs.getSelectedTabPosition()).getIcon().
                setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()), PorterDuff.Mode.SRC_IN);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                tab.getIcon().setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
                tab.getIcon().setColorFilter(Color.parseColor("#adadad"), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            tabs.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    }

    private void setupTabsIcons()
    {
        for (int i = 0; i < tabs.getTabCount(); i++)
        {
            switch (i)
            {
                case 0:
                    tabs.getTabAt(0).setIcon(R.drawable.ic_trophy);
                    break;
                case 1:
                    tabs.getTabAt(1).setIcon(R.drawable.ic_leaderboard);
                    break;
            }
        }
    }

    @Override
    public void onProfileInfoLoaded(PlayerInfo playerInfo, Level nextLevel)
    {
        if (playerInfo.getDisplayName() != null && !playerInfo.getDisplayName().isEmpty())
            txtPlayerName.setText(playerInfo.getDisplayName());


        fillPlayerData(playerInfo, nextLevel);
        prepView();
    }

    private void fillPlayerData(PlayerInfo playerInfo, Level nextLevel)
    {
        levelName.setText(playerInfo.getLevel().getName());
        if (playerInfo.getLevel().getIcon() != null)
            ImageDownloader.downloadImage(getContext(), levelLogo,
                    playerInfo.getLevel().getIcon().getFileName());

        currentPointsValue.setText(String.format(Locale.getDefault(),
                "%d", playerInfo.getAccPoints()));
        currentFrubiesValue.setText(String.format(Locale.getDefault(),
                "%d", playerInfo.getLevel().getLevelFrubies()));

        if (nextLevel != null)
        {
            frubiesForNextLevel.setText(String.format(Locale.getDefault(),
                    "%d", nextLevel.getLevelFrubies()));
            playerProgress = (playerInfo.getAccFrubies() * 100) / nextLevel.getLevelFrubies();
        } else
        {
            frubiesForNextLevel.setVisibility(View.GONE);
            levelProgress.setVisibility(View.GONE);
            nextLevelTitle.setVisibility(View.GONE);
        }

        applyAnimation();
    }

    private void applyAnimation()
    {
        ProgressBarAnimation progressBarAnimation = new ProgressBarAnimation(levelProgress, 0,
                playerProgress);
        progressBarAnimation.setDuration(700);
        progressBarAnimation.setFillAfter(true);

        levelProgress.startAnimation(progressBarAnimation);
        levelName.startAnimation(fadeIn);
        currentFrubiesValue.startAnimation(fadeIn);
        currentPointsValue.startAnimation(fadeIn);
        currentFrubiesTitle.startAnimation(fadeIn);
        currentPointTitle.startAnimation(fadeIn);
//        achievemetTitle.startAnimation(fadeIn);
    }

    @Override
    public void showLoadingIndicator()
    {
        loadingIndicatorBg.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingIndicator()
    {
        loadingIndicatorBg.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onStop()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
        {
            PowerManager pm = (PowerManager) (getContext()).getSystemService(Context.POWER_SERVICE);
            if ((pm.isInteractive()))
            {
                presenter.unsubscribe();
            }
        }
        super.onStop();
    }
}
