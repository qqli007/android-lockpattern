package haibison.android.lockpattern;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import haibison.android.lockpattern.utils.AlpSettings;
import haibison.android.lockpattern.utils.Encrypter;
import haibison.android.lockpattern.utils.LoadingView;
import haibison.android.lockpattern.utils.ResourceUtils;
import haibison.android.lockpattern.widget.LockPatternUtils;
import haibison.android.lockpattern.widget.LockPatternView;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * Created by lz on 15/10/9.
 */
public class SimpleLockPatternActivity extends Activity {

    private static final String CLASSNAME = "SimpleLockPatternActivity";

    public static final String ACTION = "action";
    public static final String ACTION_ENCRYPT = "action_encrypt";
    public static final String ACTION_OPEN_PRIVATE_FOLDER = "action_open_private_folder";

    public static final String ACTION_CREATE_PATTERN = CLASSNAME + ".CREATE_PATTERN";
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME + ".COMPARE_PATTERN";
    public static final String EXTRA_PATTERN = CLASSNAME + ".PATTERN";
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".RETRY_COUNT";

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = SECOND_IN_MILLIS;

    public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

    private enum ButtonOkCommand {
        CONTINUE, DONE
    }

    private static final int MAX_RETRIES = 5;
    private static final int MIN_WIRED_DOTS = 4;

    private TextView mTextInfo;
    private LockPatternView mLockPatternView;
    private View mFooter;
    private Button mBtnConfirm, mBtnCancel;
    private View mViewGroupProgressBar;

    private String mActionPattern;
    private ButtonOkCommand mBtnOkCmd;
    private Intent mIntentResult;
    private int mRetryCount = 0;
    private Encrypter mEncrypter;
    private LoadingView<Void, Void, Object> mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Apply theme resources
         */
        final int resThemeResources = ResourceUtils.resolveAttribute(this, R.attr.alp_42447968_theme_resources);
        if (resThemeResources == 0)
            throw new RuntimeException(
                    "Please provide theme resource via attribute `alp_42447968_theme_resources`."
                            + " For example: <item name=\"alp_42447968_theme_resources\">@style/Alp_42447968" +
                            ".ThemeResources.Light</item>");
        getTheme().applyStyle(resThemeResources, true);

        setContentView(R.layout.simple_lock_pattern_activity);

        mIntentResult = new Intent();
        setResult(RESULT_CANCELED, mIntentResult);

        initView();

    }

    private void initView() {
        mTextInfo = (TextView) findViewById(R.id.alp_42447968_textview_info);
        mLockPatternView = (LockPatternView) findViewById(R.id.alp_42447968_view_lock_pattern);

        mFooter = findViewById(R.id.alp_42447968_viewgroup_footer);
        mBtnCancel = (Button) findViewById(R.id.alp_42447968_button_cancel);
        mBtnConfirm = (Button) findViewById(R.id.alp_42447968_button_confirm);

        mViewGroupProgressBar = findViewById(R.id.alp_42447968_view_group_progress_bar);

        mLockPatternView.setOnPatternListener(mLockPatternViewListener);

        if (AlpSettings.Security.getPattern(this) == null) {
            mActionPattern = ACTION_CREATE_PATTERN;
        } else {
            mActionPattern = ACTION_COMPARE_PATTERN;
        }

        if (ACTION_CREATE_PATTERN.equals(mActionPattern)) {
            mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
            mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);
            mTextInfo.setText(R.string.alp_42447968_msg_draw_an_unlock_pattern);

            /**
             * BUTTON OK
             */
            if (mBtnOkCmd == null) mBtnOkCmd = ButtonOkCommand.CONTINUE;

        } else if (ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
            mTextInfo.setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("0-0", "onNewIntent");
        super.onNewIntent(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /**
         * Use this hook instead of onBackPressed(), because onBackPressed() is not available in API 4.
         */
        if (keyCode == KeyEvent.KEYCODE_BACK && ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
            if (mLoadingView != null) mLoadingView.cancel(true);

            finishWithNegativeResult(RESULT_CANCELED);

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /**
         * Support canceling dialog on touching outside in APIs < 11.
         *
         * This piece of code is copied from android.view.Window. You can find it by searching for methods
         * shouldCloseOnTouch() and isOutOfBounds().
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && event.getAction() == MotionEvent.ACTION_DOWN
                && getWindow().peekDecorView() != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(this).getScaledWindowTouchSlop();
            final View decorView = getWindow().getDecorView();
            boolean isOutOfBounds = (x < -slop) || (y < -slop) || (x > (decorView.getWidth() + slop))
                    || (y > (decorView.getHeight() + slop));
            if (isOutOfBounds) {
                finishWithNegativeResult(RESULT_CANCELED);
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        if (mLoadingView != null) mLoadingView.cancel(true);

        super.onDestroy();
    }


    private void doCheckAndCreatePattern(@NonNull final List<LockPatternView.Cell> pattern) {
        if (pattern.size() < MIN_WIRED_DOTS) {
            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            mTextInfo.setText(getResources().getQuantityString(
                    R.plurals.alp_42447968_pmsg_connect_x_dots, MIN_WIRED_DOTS,
                    MIN_WIRED_DOTS));
            mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }

        if (getIntent().hasExtra(EXTRA_PATTERN)) {
            /**
             * Use a LoadingView because decrypting pattern might take time...
             */
            mLoadingView = new LoadingView<Void, Void, Object>(this, mViewGroupProgressBar) {

                @Override
                protected Object doInBackground(Void... params) {
                    if (mEncrypter != null)
                        return pattern.equals(mEncrypter.decrypt(SimpleLockPatternActivity.this, getIntent()
                                .getCharArrayExtra(EXTRA_PATTERN)));
                    else
                        return Arrays.equals(getIntent().getCharArrayExtra(EXTRA_PATTERN),
                                LockPatternUtils.patternToSha1(pattern).toCharArray());
                }

                @Override
                protected void onPostExecute(Object result) {
                    super.onPostExecute(result);

                    if ((Boolean) result) {
                        mTextInfo.setText(R.string.alp_42447968_msg_your_new_unlock_pattern);
                        mBtnConfirm.setEnabled(true);
                    } else {
                        mTextInfo.setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
                        mBtnConfirm.setEnabled(false);
                        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }

            };

            mLoadingView.execute();
        } else {
            /**
             * Use a LoadingView because encrypting pattern might take time...
             */
            mLoadingView = new LoadingView<Void, Void, Object>(this, mViewGroupProgressBar) {

                @Override
                protected Object doInBackground(Void... params) {
                    return mEncrypter != null ? mEncrypter.encrypt(SimpleLockPatternActivity.this, pattern) :
                            LockPatternUtils.patternToSha1(pattern).toCharArray();
                }

                @Override
                protected void onPostExecute(Object result) {
                    super.onPostExecute(result);

                    getIntent().putExtra(EXTRA_PATTERN, (char[]) result);

                    mBtnOkCmd = ButtonOkCommand.DONE;
                    mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW / 2);

                }

            };

            mLoadingView.execute();
        }
    }

    private void doComparePattern(@NonNull final List<LockPatternView.Cell> pattern) {
        if (pattern == null) return;

        /**
         * Use a LoadingView because decrypting pattern might take time...
         */

        mLoadingView = new LoadingView<Void, Void, Object>(this, mViewGroupProgressBar) {

            @Override
            protected Object doInBackground(Void... params) {
                if (ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
                    char[] currentPattern = AlpSettings.Security.getPattern(SimpleLockPatternActivity.this);
                    if (currentPattern != null) {
                        if (mEncrypter != null)
                            return pattern.equals(mEncrypter.decrypt(SimpleLockPatternActivity.this, currentPattern));
                        else
                            return Arrays.equals(currentPattern, LockPatternUtils.patternToSha1(pattern).toCharArray());
                    }
                }

                return false;
            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);

                if ((Boolean) result) finishWithResultOk(null);
                else {
                    mRetryCount++;
                    mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);

                    if (mRetryCount >= MAX_RETRIES) {
                        finishWithNegativeResult(RESULT_FAILED);
                    } else {
                        mTextInfo.setText(R.string.alp_42447968_msg_try_again);
                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }

        };

        mLoadingView.execute();
    }

    private void finishWithResultOk(@Nullable char[] pattern) {
        if (ACTION_CREATE_PATTERN.equals(mActionPattern))
            mIntentResult.putExtra(EXTRA_PATTERN, pattern);
        else {
            /**
             * If the user was "logging in", minimum try count can not be zero.
             */
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount + 1);
        }

        setResult(RESULT_OK, mIntentResult);

        finish();
    }

    private void finishWithNegativeResult(int resultCode) {
        if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

        setResult(resultCode, mIntentResult);

        finish();
    }

    private void clearSetup() {
        mBtnOkCmd = ButtonOkCommand.CONTINUE;
        mLockPatternView.clearPattern();
        mTextInfo.setText(R.string.alp_42447968_msg_draw_an_unlock_pattern);
        mBtnConfirm.setEnabled(false);
        mFooter.setVisibility(View.INVISIBLE);
    }


    /**
     * Click listener for button Cancel.
     */
    private final View.OnClickListener mBtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            clearSetup();
        }

    };

    /**
     * Click listener for button Confirm.
     */
    private final View.OnClickListener mBtnConfirmOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (ACTION_CREATE_PATTERN.equals(mActionPattern)) {
                if (mBtnOkCmd == ButtonOkCommand.DONE) {
                    final char[] pattern = getIntent().getCharArrayExtra(EXTRA_PATTERN);
                    AlpSettings.Security.setPattern(SimpleLockPatternActivity.this, pattern);
                    finishWithResultOk(pattern);
                }
            }
        }

    };

    /**
     * This reloads the {@link #mLockPatternView} after a wrong pattern.
     */
    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPatternViewListener.onPatternCleared();
        }

    };


    /**
     * Pattern listener for LockPatternView.
     */
    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            Log.d("0-0", "onPatternStart");
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);

            if (ACTION_CREATE_PATTERN.equals(mActionPattern)) {
                mTextInfo.setText(R.string.alp_42447968_msg_release_finger_when_done);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) getIntent().removeExtra(EXTRA_PATTERN);
            } else if (ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
                mTextInfo.setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }
        }

        @Override
        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            Log.d("0-0", "onPatternDetected");
            if (ACTION_CREATE_PATTERN.equals(mActionPattern)) {
                doCheckAndCreatePattern(pattern);
            } else if (ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
                doComparePattern(pattern);
            }
        }

        @Override
        public void onPatternCleared() {
            Log.d("0-0", "onPatternCleared");
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);

            if (ACTION_CREATE_PATTERN.equals(mActionPattern)) {
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    getIntent().removeExtra(EXTRA_PATTERN);
                    mTextInfo.setText(R.string.alp_42447968_msg_draw_an_unlock_pattern);
                } else {
                    mTextInfo.setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
                    mFooter.setVisibility(View.VISIBLE);
                }
            } else if (ACTION_COMPARE_PATTERN.equals(mActionPattern)) {
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                mTextInfo.setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }
        }

        @Override
        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            // Nothing to do
        }

    };


}
