package in.org.celesta.iitp.events;

import android.content.Context;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import in.org.celesta.iitp.R;
import in.org.celesta.iitp.network.EventsRoutes;
import in.org.celesta.iitp.network.RetrofitClientInstance;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventsFragment extends Fragment {

    private static final String ARG_PARAM1 = "data";

    private String club;
    private EventsRecyclerAdapter adapter;
    private EventsViewModel viewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Context context;

    public EventsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            club = getArguments().getString(ARG_PARAM1);
        }
        viewModel = ViewModelProviders.of(this).get(EventsViewModel.class);

        if (getContext() != null)
            this.context = getContext();
        else
            NavHostFragment.findNavController(this).navigateUp();

        setEnterTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.fade));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (getActivity() != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(club + " Events");

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_events);
        swipeRefreshLayout.setOnRefreshListener(this::updateData);

        RecyclerView recyclerView = view.findViewById(R.id.rv_feed_single_type);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter = new EventsRecyclerAdapter(context, (EventsRecyclerAdapter.OnEventSelectedListener) context);
        recyclerView.setAdapter(adapter);

        observeAll();

        super.onViewCreated(view, savedInstanceState);
    }

    private void observeAll() {
        viewModel.loadAllEvents().observe(this, eventItems -> {
            List<EventItem> newList = new ArrayList<>();
            for (EventItem n : eventItems) {
                if (n.getEvClub().equals(club)) newList.add(n);
            }
            adapter.setEventItemList(newList);
        });

    }

    private void updateData() {

        EventsRoutes service = RetrofitClientInstance.getRetrofitInstance().create(EventsRoutes.class);

        Call<List<EventItem>> call = service.getAllEvents();

        call.enqueue(new Callback<List<EventItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<EventItem>> call, @NonNull Response<List<EventItem>> response) {
                if (viewModel != null) {
                    if (response.isSuccessful()) {
                        viewModel.deleteEvents();

                        List<EventItem> allItems = response.body();

                        if (allItems != null && allItems.size() > 0) {
                            for (EventItem newItem : allItems) {
                                viewModel.insert(newItem);
                            }
                        }

                        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("last_event_update_time", System.currentTimeMillis()).apply();

                    } else {
                        Log.e(getClass().getSimpleName(), "no data");
                    }
                }
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(@NonNull Call<List<EventItem>> call, @NonNull Throwable t) {
                Log.e(getClass().getSimpleName(), t.getMessage());
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
