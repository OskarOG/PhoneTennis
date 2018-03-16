package ptcorp.ptapplication.main.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.hoang8f.widget.FButton;
import ptcorp.ptapplication.R;
import ptcorp.ptapplication.game.GameActivity;
import ptcorp.ptapplication.main.MainActivity;
import ptcorp.ptapplication.main.fragments.instructionDialog.InstructionDialogFragment;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {
    private FButton mPlayButton, mBtnHowToPlay;
    private static final String TAG = "HomeFragment";
    private String username, wins, losses, winrate;
    private TextView tvPlayer, tvWins, tvLosses, tvWinrate;

    public HomeFragment() {
        // Required empty public constructor
    }

    public void setUsername(String username){
        if(tvPlayer != null){
            this.username = username;
            tvPlayer.setText(username);
        }
    }

    public void setWins(long wins){
        this.wins = String.valueOf(wins);
        if(tvWins != null)
            tvWins.setText(this.wins);
    }

    public void setLosses(long losses) {
        this.losses = String.valueOf(losses);
        if(tvLosses != null)
            tvLosses.setText(this.losses);
    }

    public void setWinrate(String winrate) {
        this.winrate = winrate;
        if(tvWinrate != null)
            tvWinrate.setText(this.winrate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        mPlayButton = v.findViewById(R.id.btnPlay);
        tvPlayer = v.findViewById(R.id.tvPlayerName);
        tvWinrate = v.findViewById(R.id.tvMyWinrate);
        tvWins = v.findViewById(R.id.tvMyWins);
        tvLosses = v.findViewById(R.id.tvMyLosses);
        tvPlayer.setText(username);
        tvWins.setText(wins);
        tvLosses.setText(losses);
        tvWinrate.setText(winrate);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GameActivity.class);
                intent.putExtra(MainActivity.USERNAME, username);
                startActivityForResult(intent, MainActivity.REQUEST_CODE);
            }
        });

        mBtnHowToPlay = v.findViewById(R.id.btnHowToPlay);
        mBtnHowToPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InstructionDialogFragment id = new InstructionDialogFragment();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.add(id, "instruction_dialog");
                ft.commit();


//                Dialog dialog = new Dialog(getContext());
//                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                dialog.setContentView(R.layout.fragment_dialog_how_to_play);

//                List<Instruction> instructions = new ArrayList<>();
//                instructions.add(new Instruction(getText(R.string.serve_title).toString(),
//                        getText(R.string.how_to_serve).toString(), R.drawable.cali_hand));
//                instructions.add(new Instruction(getText(R.string.starting_game_title).toString(),
//                        getText(R.string.lorem_ipsum).toString(), R.drawable.cali_hand));
//
//                InstructionPagerAdapter pagerAdapter = new InstructionPagerAdapter(getActivity(), instructions);
//
//                ViewPager vp = dialog.findViewById(R.id.vp_instructions);
//                vp.setAdapter(pagerAdapter);
//
//                dialog.show();
//                new InstructionDialogFragment().show(getFragmentManager(), "InstructionDialog");

            }
        });

        return v;
    }


}
