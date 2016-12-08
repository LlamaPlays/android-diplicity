package se.oort.diplicity.game;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.StringWriter;

import se.oort.diplicity.App;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;

public class GameActivity extends RetrofitActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String SERIALIZED_GAME_KEY = "serialized_game";

    public Game game;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        byte[] serializedGame = getIntent().getByteArrayExtra(SERIALIZED_GAME_KEY);
        game = (Game) unserialize(serializedGame);

        if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.size() > 0) {
            PhaseMeta pm = game.NewestPhaseMeta.get(0);
            setTitle(getResources().getString(R.string.desc_season_year_type, game.Desc, pm.Season, pm.Year, pm.Type));
        } else {
            setTitle(game.Desc);
        }

        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        showMap();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void hideAllExcept(int toShow) {
        for (int viewID : new int[]{R.id.map_view}) {
            if (viewID == toShow) {
                findViewById(viewID).setVisibility(View.VISIBLE);
            } else {
                findViewById(viewID).setVisibility(View.GONE);
            }
        }
    }

    public void showMap() {
        hideAllExcept(R.id.map_view);

        final Sendable<String> renderer = new Sendable<String>() {
            @Override
            public void send(String url) {
                Log.d("Diplicity", "Loading game map " + url);
                ((MapView) findViewById(R.id.map_view)).load(url);
            }
        };

        if (!game.Started) {
            handleReq(variantService.GetStartPhase(game.Variant), new Sendable<SingleContainer<VariantService.Phase>>() {
                @Override
                public void send(SingleContainer<VariantService.Phase> phaseSingleContainer) {
                    String url = null;
                    for (Link link : phaseSingleContainer.Links) {
                        if (link.Rel.equals("map")) {
                            url = link.URL;
                            break;
                        }
                    }
                    if (url != null) {
                        renderer.send(url);
                    } else {
                        Toast.makeText(getBaseContext(), R.string.unknown_error, Toast.LENGTH_SHORT);
                    }
                }
            }, getResources().getString(R.string.loading_start_state));
        } else {
            renderer.send(App.baseURL + "/Game/" + game.ID + " + /Phase/" + game.NewestPhaseMeta.get(0).PhaseOrdinal + "/Map");
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            showMap();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
