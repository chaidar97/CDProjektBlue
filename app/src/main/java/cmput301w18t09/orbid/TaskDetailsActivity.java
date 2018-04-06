package cmput301w18t09.orbid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;


import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;


/**
 * Activity Class that lists the details of all the recent listings
 * Tasks, as well as Assigned and Completed tasks in My Requests.
 *
 * @author Aidan Kosik, Chady Haidar, Zach Redfern
 */
public class TaskDetailsActivity extends NavigationActivity {

    public Task task;
    public Bid bid;
    public int isAssigned = 0;
    public int isBid = 0;
    public Context context = this;
    protected ArrayList<Task> taskList = new ArrayList<>();
    private DrawerLayout mDrawerLayout;
    protected boolean mine = false;
    private String id;

    private TextView textLowestBid;


    private EditText bidUsername;
    private EditText bidPrice;
    private EditText bidDescription;

    private EditText title;
    private EditText description;
    private EditText lowestBid;
    private EditText location;
    private EditText username;
    private EditText status;



    /**
     * Inflates the layout for task details. Sets the details of the task
     * being viewed. Initialises the stackView for the images of the task.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the attributes that are passed through with the intent
        setIntentArgs();



        // Find the recent listing tasks from DM
        getTaskDetails();

        // Check for errors to avoid app crashes
        if(task == null) {
            Toast.makeText(context, "This no longer exists", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (isBid == 1 && task.getAcceptedBid() == -1) {
            Toast.makeText(context, "This no longer exists", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Layout the XML that goes to the corresponding child that is being inflated
        // Then setup the generic parts.
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout frameLayout = findViewById(R.id.navigation_content_frame);

        if (isAssigned == 0) {
            inflater.inflate(R.layout.activity_new_task_details, frameLayout);
            initStackView();
        } else {
            inflater.inflate(R.layout.activity_bidded_task, frameLayout);

            bidUsername = findViewById(R.id.assignedBidUser);
            bidPrice = findViewById(R.id.assignedBidTitle);
            bidDescription = findViewById(R.id.assignedBidDescription);

            bid = task.getBidList().get(task.getAcceptedBid());
            bidPrice.setText(Double.toString(bid.getPrice()));
            bidDescription.setText(bid.getDescription());
            bidUsername.setText(bid.getProvider());
        }
        frameLayout.requestFocus();

        title = findViewById(R.id.details_task_title1);
        description = findViewById(R.id.details_task_description);
        lowestBid = findViewById(R.id.details_lowest_bid);
        location = findViewById(R.id.details_location);
        username = findViewById(R.id.usernameButton);
        status = findViewById(R.id.details_task_status);

        title.setText(task.getTitle());
        description.setText(task.getDescription());
        username.setText(task.getRequester());

        // Status
        if (task.getStatus() == Task.TaskStatus.REQUESTED) {
            status.setText("Status: Requested");
        }
        else if (task.getStatus()  == Task.TaskStatus.BIDDED) {
            status.setText("Status: Bidded");
        }
        else if (task.getStatus() == Task.TaskStatus.ASSIGNED) {
            status.setText("Status: Assigned");
        }
        else if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            status.setText("Status: Completed");
        }

        // Lowest bid
        if (task.getLowestBid() != -1) {
            lowestBid.setText("Lowest Bid: " + Double.toString(task.getLowestBid()));
        }
        else {
            lowestBid.setText("Lowest Bid: N/A");
        }

        // Location
        if (task.getStringLocation() == null) {
            location.setText("No Location Specified");
        }
        else {
            location.setText(task.getStringLocation());
        }



        // Set the tasks values
        //setTaskValues();

//        // Set the username button
//        usernameBtn = findViewById(R.id.usernameButton);
//        usernameBtn.setText(task.getRequester());


//        // Setting up the assigned bid layout
//        // 1 means assigned, 2 means completed, 0 is for requested
//        if(isAssigned == 1 || isAssigned == 2) {
//            isAssignedTask();
//        }

//        // Check to see if this task is owned by the user opening it
//        if (task.getRequester().equals(this.thisUser)) {
//            mine = true;
//        }
    }

    /**
     * Initializes the stack view and sets its on click listener to open
     * the image full screen when pressed.
     */
    private void initStackView() {
        StackView stackView = findViewById(R.id.ImageStack);
        stackView.setInAnimation(this, android.R.animator.fade_in);
        stackView.setOutAnimation(this, android.R.animator.fade_out);
        ImageViewAdapter imageAdapter = new ImageViewAdapter(this, task.getPhotoList());
        stackView.setAdapter(imageAdapter);
        stackView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<Bitmap> temp;

                // Get the bitmap that was tapped from the photo list
                temp = task.getPhotoList();
                Bitmap image = temp.get(position);

                // Create a new intent and send it the byte array for bitmap
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();
                intent.putExtra("BitmapImage",bytes);
                intent.putExtra("isMyTask", 0);
                intent.putExtra("_id", task.getID());
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
    }

    /**
     * If the task that is being viewed is completed or is assigned
     * it needs to make these changes.
     */
    private void isAssignedTask() {
//        int b = task.getAcceptedBid();
        bid = task.getBidList().get(task.getAcceptedBid());
        textLowestBid = findViewById(R.id.details_lowest_bid);

        // Show the buttons if the task is Assigned, only if the network is available
        if (isAssigned == 1) {
            Button fulfilledBtn = (Button) findViewById(R.id.fulfilledButton);
            Button repostBtn = (Button) findViewById(R.id.repostButton);
            fulfilledBtn.setVisibility(View.VISIBLE);
            repostBtn.setVisibility(View.VISIBLE);
        }

        // Set necessary elements to visible
        bidUsername.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);

        // Set the text to the items
        textLowestBid.setText("Bid price:\n$" + String.format("%.2f", bid.getPrice()));
        bidUsername.setText(bid.getProvider());
        description.setText(bid.getDescription());
    }

    /**
     * Setup the onClick listener for username tap
     */
    public void setTitleListener(View view) {
        openUserInfo(bid.getProvider());

    }

    /**
     * Get the arguments that were passed with the intent.
     */
    private void setIntentArgs() {

        // Use the id of the task to get it from the Data Manager
        try {
            id = getIntent().getStringExtra("_id");
        } catch (Error e) {

        }
        try {
            isAssigned = getIntent().getIntExtra("isAssigned", 0);
        } catch(Error e) {
        }
        try {
            isBid = getIntent().getIntExtra("isBid", 0);
        } catch(Error e) {
        }
    }

    /**
     * Find and then set the text for each of the text views.
     */
    private void setTaskValues() {

        // Find the text views in the layout
        TextView task_title = findViewById(R.id.details_task_title);
        TextView task_description = findViewById(R.id.details_task_description);
        TextView text_task_status = findViewById(R.id.details_task_status);

        // Set the task title and description
        task_title.setText(task.getTitle());
        task_description.setText(task.getDescription());
        text_task_status.setText(task.getStatus().toString());

        if (isAssigned != 0) {
            // Set the bid
            setBid((TextView) findViewById(R.id.details_lowest_bid));
        }
    }

    /**
     * Function for when an options menu item is selected.
     *
     * @param item
     * @return boolean if the item was selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * sets the task status to completed and kills the activity
     *
     * @param view The Activity view
     */
    public void fulfilled(View view) {

        // Don't allow the user to set tasks to complete when offline
        if (!DataManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "Cannot set tasks to complete when offline", Toast.LENGTH_SHORT).show();
            return;
        }

        task.setStatus(Task.TaskStatus.COMPLETED);

        // Alter the task in the backup list to match the status of the fulfilled task
        ListIterator<Task> it = DataManager.backupTasks.listIterator();
        while (it.hasNext()) {
            if (it.next().getID().equals(task.getID())) {
                it.set(task);
                break;
            }
        }

        save();
        finish();
    }

    /**
     * Sets the task status to requested/bidded and deletes the bid
     *
     * @param view The activity view
     */
    public void repost(View view) {

        // Don't allow user to repost tasks when offline
        if (!DataManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "Cannot repost tasks when offline", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove the previously accepted bid
        ArrayList<Bid> temp = new ArrayList<>();
        temp = task.getBidList();
        temp.remove(task.getAcceptedBid());
        task.setBidList(temp);
        task.acceptBid(-1);
        if(temp.size() == 0) {
            task.setStatus(Task.TaskStatus.REQUESTED);
        } else {
            task.setStatus(Task.TaskStatus.BIDDED);
        }
        // Alter the task in the backup list to match the status of the reposted task
        /*ListIterator<Task> it = DataManager.backupTasks.listIterator();
        while (it.hasNext()) {
            if (it.next().getID().equals(task.getID())) {
                it.set(task);
                break;
            }
        }*/

        save();
        finish();
    }

    public void openBidDialog(View view) {

        //Inflate the dialog
        LayoutInflater layoutInflater = this.getLayoutInflater();
        final View dialog_view = layoutInflater.inflate(R.layout.dialog_add_bid, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(TaskDetailsActivity.this).setView(dialog_view);
        final AlertDialog dialog = builder.create();
        dialog.show();


        final EditText etPrice = dialog_view.findViewById(R.id.et_bidprice);
        final EditText etDescription = dialog_view.findViewById(R.id.et_biddescription);
        final Button cancel = dialog_view.findViewById(R.id.bt_cancel);
        final Button bid = dialog_view.findViewById(R.id.bt_bid);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        //
        bid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Inform the user if they attempt to make a bid offline
                if (!DataManager.isNetworkAvailable(getBaseContext() )) {
                    Toast.makeText(getBaseContext(), "Bids cannot be placed while offline", Toast.LENGTH_SHORT).show();
                    return;
                } else if(task == null) {
                    Toast.makeText(context, "This task no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Log.i("BID", "title is empty: " + etPrice.getText().toString());
                Log.i("BID", "description is empty: " + etDescription.getText().toString());

                if (description.getText().length() > 300) {
                    Toast.makeText(getBaseContext(), "Bid description cannot exceed 300 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Let the user know this task has been completed and bidding is closed.
                if(task.getStatus() == Task.TaskStatus.COMPLETED){
                    Toast.makeText(getBaseContext(), "This task has already been completed!", Toast.LENGTH_SHORT).show();
                } else if (!etPrice.getText().toString().isEmpty() && !etDescription.getText().toString().isEmpty()) {
                    Double price = Double.parseDouble(etPrice.getText().toString());
                    if(price > 1000000) {
                        Toast.makeText(getBaseContext(), "You cannot bid over $1000000.", Toast.LENGTH_SHORT).show();
                    } else {
                        ArrayList<Bid> temp = task.getBidList();
                        // Check if the user had a previous bid and delete it if so
                        for(Bid b: temp) {
                            if(b.getProvider().toLowerCase().equals(thisUser.toLowerCase())){
                                task.removeBid(b);
                            }
                        }
                        // Add the new bid to the bid list
                        Bid bid = new Bid(LoginActivity.getCurrentUsername(), price, etDescription.getText().toString());
                        task.addBid(bid);
                        task.setStatus(Task.TaskStatus.BIDDED);
                        // Notify the Task that the requester needs to receive a notification
                        if (!task.getShouldNotify()) {
                            task.setShouldNotify(true);
                        }
                        DataManager.updateTasks updateTasks = new DataManager.updateTasks(getBaseContext());
                        updateTasks.execute(taskList);

                    }
                } else {
                    Toast.makeText(getBaseContext(), "You need to fill out both bid fields properly", Toast.LENGTH_SHORT).show();
                    return;
                }


                dialog.dismiss();
                finish();
            }
        });


    }

    /**
     * Opens the user info dialog when pressed
     */
    public void openUserInfo(String username) {

        // Inform the user if they attempt to get user information while offline
        if (!DataManager.isNetworkAvailable(this )) {
            Toast.makeText(this, "User information cannot be fetched while offline", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString("username", username);

        // Let the dialog know if the user being clicked can be reviewed
        if (getIntent().getBooleanExtra("cameFromCompletedBid", false)) {

            getTaskDetails();

            if (task.getIsReviewedByProvider() == false) {
                bundle.putBoolean("canReview", true);
                bundle.putString("reviewType", "Requester");
                bundle.putString("taskID", task.getID());
            }
            else {
                bundle.putBoolean("canReview", false);
            }

        }
        else if (getIntent().getBooleanExtra("cameFromCompletedRequest", false)) {

            getTaskDetails();

            if (task.getIsReviewedByRequester() == false) {
                bundle.putBoolean("canReview", true);
                bundle.putString("reviewType", "Provider");
                bundle.putString("taskID", task.getID());
            }
            else {
                bundle.putBoolean("canReview", false);
            }

        }
        else {
            bundle.putBoolean("canReview", false);
        }

        UserProfileDialog dialog = new UserProfileDialog();
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "User Profile Dialog");

    }

    /**
     * Saves the current task in the database
     */
    private void save() {
        ArrayList<Task> n = new ArrayList<>();
        n.add(task);
        DataManager.updateTasks object = new DataManager.updateTasks(this);
        object.execute(n);
    }

    /**
     * Find the lowest bid on the displayed task and then display its amount
     * in the text view, or display your bid
     *
     * @param text_lowest_bid The textview for the lowest bid
     */
    public void setBid(TextView text_lowest_bid) {
        Bid lowest_bid = null;
        if (task == null) {
            Log.i("MSG", "task isa null here");
        }

        // Display your bid price
        if (isBid == 1) {
            ArrayList<Bid> temp;
            temp = task.getBidList();
            for (Bid b : temp) {
                if (b.getProvider().toLowerCase().equals(thisUser.toLowerCase())) {
                    bid = b;
                }
            }
            // Set your bid price, username, and description
            text_lowest_bid.setText("Your bid:\n$" + String.format("%.2f", bid.getPrice()));
            bidUsername.setVisibility(View.VISIBLE);
            bidUsername.setText(bid.getProvider());
            description.setVisibility(View.VISIBLE);
            description.setText(bid.getDescription());
        } else {
            // Check if the task is completed
            if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.ASSIGNED) {
                text_lowest_bid.setText("TASK FULFILLED");
            } else {
                if (task.getBidList().size() == 0) {
                    text_lowest_bid.setText("Price:\n$" + String.format("%.2f", task.getPrice()));
                } else {
                    // Find the lowest bid to display
                    for (Bid bid : task.getBidList()) {
                        if (lowest_bid != null) {
                            if (bid.getPrice() < lowest_bid.getPrice()) {
                                lowest_bid = bid;
                            }
                        } else {
                            lowest_bid = bid;
                        }
                    }
                    if (lowest_bid != null) {
                        text_lowest_bid.setText("Lowest Bid:\n$" + String.format("%.2f", lowest_bid.getPrice()));
                    }
                }
            }
        }
    }

    /**
     * Gets the details of the task being loaded from the DM and then
     * stores them in taskList.
     */
    protected void getTaskDetails() {
        // Use the id of the task to get it from the Data Manager
        id = getIntent().getStringExtra("_id");
        // Get the task that was clicked
        ArrayList<String> query = new ArrayList<>();
        query.add("and");
        query.add("_id");
        query.add(id);
        DataManager.getTasks getTasks = new DataManager.getTasks(this);
        getTasks.execute(query);
        try {
            taskList = getTasks.get();

            // If there is no network available, fetch the backup task
            if (!DataManager.isNetworkAvailable(this )) {
                task = new Gson().fromJson(getIntent().getStringExtra("backupTask"), Task.class);
            }
            else {
                if (taskList.size() > 0) {
                    task = taskList.get(0);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


}
