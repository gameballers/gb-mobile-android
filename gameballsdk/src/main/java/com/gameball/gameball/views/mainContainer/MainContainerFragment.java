package com.gameball.gameball.views.mainContainer;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.response.ClientBotSettings;
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

    TabsAdapter tabsAdapter;
    ClientBotSettings clientBotSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    private void initComponents() {
        tabsAdapter = new TabsAdapter(getChildFragmentManager());
        clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main_container, container, false);
        initView();
        setupBotSettings();
        prepView();
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
    }

    private void setupBotSettings()
    {
        headerParent.setBackgroundColor(Color.parseColor(clientBotSettings.getBotMainColor()));
        tabs.setSelectedTabIndicatorColor(Color.parseColor(clientBotSettings.getBotMainColor()));
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
                setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                tab.getIcon().setColorFilter(Color.parseColor(clientBotSettings.getBotMainColor()), PorterDuff.Mode.SRC_IN);
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

    }

    private void setupTabsIcons() {
        tabs.getTabAt(0).setIcon(R.drawable.ic_profile);
//        tabs.getTabAt(1).setIcon(R.drawable.ic_flag_black);
        tabs.getTabAt(1).setIcon(R.drawable.ic_trophy);
//        tabs.getTabAt(3).setIcon(R.drawable.ic_trophy);
    }

    @Override
    public void showLoadingIndicator() {

    }

    @Override
    public void hideLoadingIndicator() {

    }
}
