package com.gameball.gameball.views.mainContainer;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.DisplayUtils;

public class MainContainerFragment extends DialogFragment implements MainContainerContract.View {

    private View rootView;
    private ImageView gameballLogo;
    private TextView txtPlayerName;
    private TextView txtPlayerEmail;
    private ImageButton btnClose;
    private TabLayout tabs;
    private ViewPager viewPager;
    private ConstraintLayout headerParent;
    ProgressBar loadingIndicator;

    TabsAdapter tabsAdapter;
    MainContainerContract.Presenter presenter;
    ClientBotSettings clientBotSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    private void initComponents() {
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
        tabsAdapter = new TabsAdapter(getChildFragmentManager(), clientBotSettings.isEnableLeaderboard());
        presenter = new MainContainerPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main_container, container, false);
        initView();
        setupBotSettings();
//        prepView();
        presenter.getPlayerInfo();
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        // the content
        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // creating the fullscreen dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
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

    private void initView() {
        gameballLogo = rootView.findViewById(R.id.gameball_logo);
        txtPlayerName = rootView.findViewById(R.id.txt_player_name);
        txtPlayerEmail = rootView.findViewById(R.id.txt_player_email);
        btnClose = rootView.findViewById(R.id.btn_close);
        tabs = rootView.findViewById(R.id.tabs);
        viewPager = rootView.findViewById(R.id.view_pager);
        headerParent = rootView.findViewById(R.id.header);
        loadingIndicator = rootView.findViewById(R.id.loading_indicator);
    }

    private void setupBotSettings()
    {
        headerParent.setBackgroundColor(Color.parseColor(clientBotSettings.getButtonBackgroundColor()));
        tabs.setSelectedTabIndicatorColor(Color.parseColor(clientBotSettings.getButtonBackgroundColor()));
//        LayerDrawable loadingIndicatorDrawable = (LayerDrawable) loadingIndicator.getProgressDrawable();
        loadingIndicator.getIndeterminateDrawable().setColorFilter(Color.parseColor(clientBotSettings.getButtonBackgroundColor()),
                PorterDuff.Mode.SRC_IN);
    }

    private void prepView() {
        viewPager.setAdapter(tabsAdapter);
        tabs.setupWithViewPager(viewPager);
        tabs.setSelectedTabIndicatorHeight((int) DisplayUtils.convertDpToPixel(2));
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private void setupTabsIcons() {
        for (int i =0; i < tabs.getTabCount();i++)
        {
            switch (i)
            {
                case 0:
                    tabs.getTabAt(0).setIcon(R.drawable.ic_profile);
                break;
                case 1:
                    tabs.getTabAt(1).setIcon(R.drawable.ic_trophy);
                    break;
//        tabs.getTabAt(1).setIcon(R.drawable.ic_flag_black);
//        tabs.getTabAt(3).setIcon(R.drawable.ic_trophy);
            }
        }
    }

    @Override
    public void setPlayerName(String name)
    {
        txtPlayerName.setText(name);
    }

    @Override
    public void setPlayerEmail(String  email)
    {
        txtPlayerEmail.setText(email);
    }

    @Override
    public void onProfileInfoLoaded(PlayerInfo playerInfo)
    {
        if(playerInfo.getName() != null && !playerInfo.getName().isEmpty())
            txtPlayerName.setText(playerInfo.getName());
        prepView();
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
    public void onStop()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
        {
            PowerManager pm = (PowerManager) (getContext()).getSystemService(Context.POWER_SERVICE);
            if((pm.isInteractive()))
            {
                presenter.unsubscribe();
            }
        }
        super.onStop();
    }
}
