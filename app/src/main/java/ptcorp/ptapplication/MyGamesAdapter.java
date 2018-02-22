package ptcorp.ptapplication;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Pontus on 2018-02-22.
 */

public class MyGamesAdapter extends RecyclerView.Adapter<MyGamesAdapter.GamesViewholder> {

    private List<MyGamesScore> games;

    public MyGamesAdapter(List<MyGamesScore> games){
        this.games = games;
    }

    @Override
    public GamesViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.my_games_item, parent, false);
        return new GamesViewholder(v);
    }

    @Override
    public void onBindViewHolder(GamesViewholder holder, int position) {
        MyGamesScore game = games.get(position);

        holder.tvPlayer1.setText(game.getPlayer1());
        holder.tvPlayer2.setText(game.getPlayer2());
        holder.tvScore1.setText(game.getScore1());
        holder.tvScore2.setText(game.getScore2());
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    class GamesViewholder extends RecyclerView.ViewHolder{

        private TextView tvPlayer1, tvPlayer2, tvScore1, tvScore2;

        public GamesViewholder(View itemView) {
            super(itemView);

            tvPlayer1 = itemView.findViewById(R.id.tvPlayer1);
            tvPlayer2 = itemView.findViewById(R.id.tvPlayer2);
            tvScore1 = itemView.findViewById(R.id.tvScore1);
            tvScore2 = itemView.findViewById(R.id.tvScore2);
        }
    }
}