package com.rabidllamastudios.avigate;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity that allows the user to manage their saved craft profiles (and configurations)
 */
public class SelectCraftActivity extends AppCompatActivity {

    private SharedPreferencesManager mSharedPreferencesManager;
    private List<CraftProfile> mCraftProfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initialize the data structure used in the CardView UI
        initializeCraftProfiles();

        //Configure RecyclerView and set up RecyclerView UI
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(SelectCraftActivity.this);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview_craft_profiles);
        recyclerView.setLayoutManager(layoutManager);

        //Configure RecyclerViewAdapter to handle UI and data logic on a per CardView basis
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(mCraftProfiles);
        recyclerView.setAdapter(recyclerViewAdapter);
        //Since the size of each child CardView does not change, optimize RecyclerView
        recyclerView.setHasFixedSize(true);
    }

    //Creates a new craft profile when the "+" (plus) floating action button is tapped
    public void createNewCraftProfile(View view) {
        final EditText editText = new EditText(this);
        AlertDialog.Builder alertDialogBuilder = getEditTextAlertDialogBuilder(editText);
        alertDialogBuilder.setTitle("New Craft Profile");
        alertDialogBuilder.setMessage("Craft Name");
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String craftName = editText.getText().toString();
                if (mSharedPreferencesManager.hasCraft(craftName)) {
                    //TODO: warn the user about conflicting names
                } else {
                    mSharedPreferencesManager.addCraft(craftName);
                    mCraftProfiles.add(new CraftProfile(craftName, "Not configured",
                            R.drawable.ic_airplanemode_active_black_48dp));
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        //Automatically bring up the keyboard
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    //Returns an EditText AlertDialogBuilder with a centered EditText view in the message body
    //Also sets a default Cancel button
    private AlertDialog.Builder getEditTextAlertDialogBuilder(final EditText editText) {
        final AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(SelectCraftActivity.this);
        FrameLayout frameLayout = new FrameLayout(SelectCraftActivity.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //TODO don't use hard-coded values
        params.leftMargin = 67;
        params.rightMargin = 67;
        editText.setSingleLine();
        editText.setLayoutParams(params);
        editText.setSelection(editText.getText().length());
        frameLayout.addView(editText);
        alertDialogBuilder.setView(frameLayout);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
        return alertDialogBuilder;
    }

    //Initializes craftProfiles via SharedPreferences settings - this is used for the CardView UI
    private void initializeCraftProfiles() {
        mCraftProfiles = new ArrayList<>();
        mSharedPreferencesManager = new SharedPreferencesManager(this);
        Set<String> craftNames = mSharedPreferencesManager.getCraftList();
        for (String craftName : craftNames) {
            //TODO implement calibration timestamps
            mCraftProfiles.add(new CraftProfile(craftName, "Not calibrated",
                    R.drawable.ic_airplanemode_active_black_48dp));
        }
    }


    //RecyclerViewAdapter class that handles the UI/data logic coupling for the RecyclerView
    private class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {

        //A list of the craft profiles
        private List<CraftProfile> mCraftProfileList;

        //RecyclerViewHolder class that handles the UI/data logic coupling for each CardView
        public class RecyclerViewHolder extends RecyclerView.ViewHolder {
            Button mConfigureButton;
            Button mFlyButton;
            ImageButton mOverflowMenu;
            ImageView mCraftProfileImage;
            TextView mCalibrationDetails;
            TextView mCraftProfileName;

            public RecyclerViewHolder(View itemView) {
                super(itemView);
                mCraftProfileName = (TextView) itemView.findViewById(R.id.row_craft_profile_name);
                mCalibrationDetails = (TextView) itemView.findViewById(
                        R.id.row_craft_calibration_details);
                mCraftProfileImage = (ImageView) itemView.findViewById(
                        R.id.iv_row_craft_profile_image);
                mConfigureButton = (Button) itemView.findViewById(R.id.button_configure);
                mFlyButton = (Button) itemView.findViewById(R.id.button_fly);
                mOverflowMenu = (ImageButton) itemView.findViewById(
                        R.id.ib_row_overflow_menu_image);
            }
        }

        //Initialize mCraftProfileList
        public RecyclerViewAdapter(List<CraftProfile> craftProfileList) {
            mCraftProfileList = craftProfileList;
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Required method to set the layout on creation
            View view = LayoutInflater.from(
                    //TODO correct null warning
                    parent.getContext()).inflate(R.layout.row_cardview_craft_profile, null);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, final int position) {
            //Configure UI elements on a per CraftProfile basis
            final String craftName = mCraftProfileList.get(position).getName();
            holder.mCraftProfileName.setText(craftName);
            holder.mCalibrationDetails.setText(
                    mCraftProfileList.get(position).getCalibrationDetails());
            holder.mCraftProfileImage.setImageResource(
                    mCraftProfileList.get(position).getCraftProfilePic());
            //Set the behavior for the Configure button
            holder.mConfigureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent configureArduinoIntent = new Intent(SelectCraftActivity.this,
                            ConfigureArduinoActivity.class);
                    configureArduinoIntent.putExtra(SharedPreferencesManager.KEY_CRAFT_NAME,
                            craftName);
                    startActivity(configureArduinoIntent);
                }
            });
            //Set the behavior for the Fly button
            holder.mFlyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent controllerIntent = new Intent(SelectCraftActivity.this,
                            ControllerActivity.class);
                    controllerIntent.putExtra(SharedPreferencesManager.KEY_CRAFT_NAME,
                            craftName);
                    startActivity(controllerIntent);
                }
            });
            //Set the behavior for the overflow menu
            holder.mOverflowMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(SelectCraftActivity.this, v);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.item_popup_cardview_rename:
                                    showRenameCraftAlertDialog(holder);
                                    break;
                                case R.id.item_popup_cardview_delete:
                                    showDeleteCraftAlertDialog(holder, position);
                                    break;
                                default:
                            }
                            return true;
                        }
                    });
                    popupMenu.inflate(R.menu.menu_popup_cardview);
                    popupMenu.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            //Required method that returns the number of craft profiles in the RecyclerView
            return mCraftProfileList.size();
        }

        //Displays an AlertDialog that allows the user to edit the exist craft profile name
        private void showRenameCraftAlertDialog(final RecyclerViewHolder holder) {
            final EditText editText = new EditText(SelectCraftActivity.this);
            editText.setText(holder.mCraftProfileName.getText().toString());
            AlertDialog.Builder alertDialogBuilder =
                    getEditTextAlertDialogBuilder(editText);
            alertDialogBuilder.setTitle("Rename Craft");
            alertDialogBuilder.setMessage("Craft Name");
            alertDialogBuilder.setPositiveButton("Set",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newCraftName = editText.getText().toString();
                            if (mSharedPreferencesManager.hasCraft(newCraftName)) {
                                //TODO: Warn user about other craft
                            } else {
                                mSharedPreferencesManager.updateCraftName(
                                        holder.mCraftProfileName.getText().toString(),
                                        newCraftName);
                                holder.mCraftProfileName.setText(editText.getText().toString());
                            }
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            //Automatically bring up the keyboard
            alertDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
        }

        //Prompts the user to confirm they wish to delete this craft profile
        private void showDeleteCraftAlertDialog(RecyclerViewHolder holder, final int position) {
            final String craftName = holder.mCraftProfileName.getText().toString();
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(SelectCraftActivity.this);
            alertDialogBuilder.setTitle("Confirm Delete");
            alertDialogBuilder.setMessage("Are you sure you want to delete the craft profile "
                    + craftName + "?");
            alertDialogBuilder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
            alertDialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSharedPreferencesManager.removeCraft(craftName);
                    mCraftProfileList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, mCraftProfileList.size());
                }
            });
            alertDialogBuilder.show();
        }
    }

    //Class that holds the craft profile data for each CardView
    private class CraftProfile {

        private int mCraftProfilePic;

        private String mCalibrationTimestamp;
        private String mName;

        //Takes a name, calibrationTimestamp, and a profile picture resource
        public CraftProfile(String name, String calibrationTimestamp, int craftProfilePic) {
            mName = name;
            mCraftProfilePic = craftProfilePic;
            mCalibrationTimestamp = calibrationTimestamp;
        }

        //Returns the name of the craft profile
        public String getName() {
            return mName;
        }

        //Sets the name of the craft profile
        public void setName(String name) {
            mName = name;
        }

        //Returns the calibration details (String)
        public String getCalibrationDetails() {
            return mCalibrationTimestamp;
        }

        //TODO implement setting a custom profile picture
        //Returns the craft profile picture resource int
        public int getCraftProfilePic() {
            return mCraftProfilePic;
        }
    }

}