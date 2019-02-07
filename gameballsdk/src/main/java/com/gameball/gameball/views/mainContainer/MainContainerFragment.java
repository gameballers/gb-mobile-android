package com.gameball.gameball.views.mainContainer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.gameball.gameball.R;

public class MainContainerFragment extends Fragment implements MainContainerContract.View
{

    private View rootView;
    private ImageView gameballLogo;
    private TextView txtPlayerName;
    private TextView txtPlayerEmail;
    private ImageButton btnClose;
    private TabLayout tabs;
    private ViewPager viewPager;

    TabsAdapter tabsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initComponents();
    }

    private void initComponents()
    {
        tabsAdapter = new TabsAdapter(getChildFragmentManager());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_main_container, container, false);
        initView();
        prepView();
        return rootView;
    }

    private void initView()
    {
        gameballLogo = (ImageView) rootView.findViewById(R.id.gameball_logo);
        txtPlayerName = (TextView) rootView.findViewById(R.id.txt_player_name);
        txtPlayerEmail = (TextView) rootView.findViewById(R.id.txt_player_email);
        btnClose = (ImageButton) rootView.findViewById(R.id.btn_close);
        tabs = (TabLayout) rootView.findViewById(R.id.tabs);
        viewPager =  rootView.findViewById(R.id.view_pager);
    }

    private void prepView()
    {
        viewPager.setAdapter(tabsAdapter);
        tabs.setupWithViewPager(viewPager);
        setupTabsIcons();

    }

    private void setupTabsIcons()
    {
        tabs.getTabAt(0).setIcon(R.drawable.ic_news_black);
        tabs.getTabAt(1).setIcon(R.drawable.ic_profile_black);
        tabs.getTabAt(2).setIcon(R.drawable.ic_flag_black);
        tabs.getTabAt(3).setIcon(R.drawable.ic_profile_black);
    }
    @Override
    public void showLoadingIndicator()
    {

    }

    @Override
    public void hideLoadingIndicator()
    {

    }
}
