package com.daquexian.flexiblerichtextview;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kbiakov.codeview.CodeView;

import static com.daquexian.flexiblerichtextview.Tokenizer.ATTACHMENT;
import static com.daquexian.flexiblerichtextview.Tokenizer.BOLD_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.BOLD_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.CENTER_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.CENTER_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.CODE_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.CODE_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.COLOR_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.COLOR_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.CURTAIN_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.CURTAIN_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.DELETE_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.DELETE_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.END;
import static com.daquexian.flexiblerichtextview.Tokenizer.FORMULA;
import static com.daquexian.flexiblerichtextview.Tokenizer.ICON;
import static com.daquexian.flexiblerichtextview.Tokenizer.IMAGE;
import static com.daquexian.flexiblerichtextview.Tokenizer.ITALIC_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.ITALIC_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.PLAIN;
import static com.daquexian.flexiblerichtextview.Tokenizer.QUOTE_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.QUOTE_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.TABLE;
import static com.daquexian.flexiblerichtextview.Tokenizer.TITLE_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.TITLE_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.TOKEN;
import static com.daquexian.flexiblerichtextview.Tokenizer.UNDERLINE_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.UNDERLINE_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.URL_END;
import static com.daquexian.flexiblerichtextview.Tokenizer.URL_START;
import static com.daquexian.flexiblerichtextview.Tokenizer.tokenizer;

/**
 * Created by jianhao on 16-8-26.
 */
public class FlexibleRichTextView extends LinearLayout {
    private final static String TAG = "FlexibleRichTextView";
    static final int MAX_IMAGE_WIDTH = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);

    private Context mContext;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private int mConversationId;
    private List<Attachment> mAttachmentList;
    private OnViewClickListener mOnViewClickListener;
    private OnSetTokenResultCallback mOnSetTokenResultCallback;

    private List<Tokenizer.TOKEN> mTokenList;
    private int mTokenIndex;

    private boolean mCenter;

    private boolean mShowRemainingAtt = true;

    private int mQuoteViewId = R.layout.default_quote_view;

    private int textColor;

    private int maxLines;

    private int linesLimit = 0;
    private float linesWidth = SizeUtil.screenWidth();
    private float lineSpacing = 10;

    private boolean textIsSelectable = false;
    private boolean pureText = false;

    private float textSize;

    private LaTeXtView textView;

    private List<LaTeXtView> textViewList;

    private static IFlexibleRichImageLoader mImageLoader = null;

    public static void setImageLoader(IFlexibleRichImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public FlexibleRichTextView(Context context) {
        this(context, null, true);
    }

    @SuppressWarnings("unused")
    public FlexibleRichTextView(Context context, OnViewClickListener onViewClickListener) {
        this(context, onViewClickListener, true);
    }

    @SuppressWarnings("unused")
    public FlexibleRichTextView(Context context, OnViewClickListener onViewClickListener, boolean showRemainingAtt) {
        super(context);
        init(context, onViewClickListener, showRemainingAtt);
    }

    public FlexibleRichTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
        initAttr(attributeSet);
    }

    public FlexibleRichTextView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context);
        initAttr(attributeSet);
    }

    private void initAttr(AttributeSet attributeSet) {
        TypedArray a = mContext.obtainStyledAttributes(attributeSet, R.styleable.FlexibleRichTextView);
        textColor = a.getColor(R.styleable.FlexibleRichTextView_textColor, 0XFF000000);
        textSize = a.getDimension(R.styleable.FlexibleRichTextView_textSize, SizeUtil.sp2px(20));
        maxLines = a.getInteger(R.styleable.FlexibleRichTextView_maxLine, -1);
        pureText = a.getBoolean(R.styleable.FlexibleRichTextView_frtv_pureText, pureText);
        textIsSelectable = a.getBoolean(R.styleable.FlexibleRichTextView_textIsSelectable, textIsSelectable);
        linesLimit = a.getInteger(R.styleable.FlexibleRichTextView_frtv_linesLimit, 0);
        linesWidth = a.getDimension(R.styleable.FlexibleRichTextView_frtv_linesWidth, linesWidth);
        lineSpacing = a.getDimension(R.styleable.FlexibleRichTextView_frtv_lineSpacing, lineSpacing);
        a.recycle();
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void changeTextColor(int textColor) {
        if (textViewList != null) {
            for (LaTeXtView laTeXtView : textViewList) {
                laTeXtView.setTextColor(textColor);
            }
        }
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextIsSelectable(boolean textIsSelectable) {
        this.textIsSelectable = textIsSelectable;
    }

    public boolean isTextIsSelectable() {
        return textIsSelectable;
    }

    public void setLinesLimit(int linesLimit) {
        this.linesLimit = linesLimit;
    }

    public int getLinesLimit() {
        return linesLimit;
    }

    public void setLinesWidth(float linesWidth) {
        this.linesWidth = linesWidth;
    }

    public float getLinesWidth() {
        return linesWidth;
    }

    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setPureText(boolean pureText) {
        this.pureText = pureText;
    }

    public boolean isPureText() {
        return pureText;
    }

    public void setText(CharSequence text) {
        setText(text, new ArrayList<Attachment>());
    }

    public void setText(CharSequence text, List<Attachment> attachmentList) {
        mAttachmentList = attachmentList;
        mTokenList = tokenizer(text, mAttachmentList);
        setToken(mTokenList, attachmentList);
    }

    public void setOnSetTokenResultCallback(OnSetTokenResultCallback mOnSetTokenResultCallback) {
        this.mOnSetTokenResultCallback = mOnSetTokenResultCallback;
    }

    public void setText(String text) {
        setText(text, new ArrayList<Attachment>());
    }

    public void setText(String text, List<Attachment> attachmentList) {
        text = text.replaceAll("\u00AD", "");
        mAttachmentList = attachmentList;
        mTokenList = tokenizer(text, mAttachmentList);
        setToken(mTokenList, attachmentList);
    }

    public String getText() {
        if (null == textView) {
            return "";
        }
        return textView.getText().toString();
    }

    public void setToken(List<TOKEN> tokens, List<Attachment> attachmentList) {
        mAttachmentList = attachmentList;
        mTokenList = tokens;
        for (TOKEN token : tokens) {
            if (token instanceof ATTACHMENT) {
                mAttachmentList.remove(((ATTACHMENT) token).attachment);
            }
        }
        resetTokenIndex();
        if (linesLimit > 0) {
            setTokenResult();
        } else {
            setTokenResult();
        }
    }

    private void setTokenResult() {
        removeAllViews();
        final List<Object> result = until(END.class);
        if (mShowRemainingAtt) {
            for (Attachment att : mAttachmentList) {
                append(result, attachment(att));
            }
        }
        if (result == null) {
            return;
        }
        textViewList = new ArrayList<>();
        boolean useLinesLimit = linesLimit > 0;
        int lines = linesLimit;
        for (final Object o : result) {
            if (useLinesLimit && lines <= 0) {
                break;
            }
            if (o instanceof TextWithFormula) {
                final TextWithFormula textWithFormula = (TextWithFormula) o;
                textView = new LaTeXtView(mContext);
                textView.setTextIsSelectable(textIsSelectable);
                textView.setOnFormulaParsedListener(new LaTeXtView.OnFormulaParsedListener() {
                    @Override
                    public void onFormulaParsed(@NonNull SpannableStringBuilder builder) {
                        if (mOnSetTokenResultCallback != null) {
                            mOnSetTokenResultCallback.onText(builder);
                        }
                    }
                });
                textView.setTextSize(SizeUtil.px2sp(textSize));
                textView.setTextColor(textColor);
                textView.setLineSpacing(lineSpacing, 1);
                textView.setTextWithFormula(textWithFormula);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mOnViewClickListener) {
                            mOnViewClickListener.onViewClick();
                        }
                    }
                });
                textViewList.add(textView);
                myAddView(textView);
                if (useLinesLimit) {
                    textView.setMaxLines(linesLimit);
                    lines -= TextViewLinesUtil.getTextViewLines(textView, (int) linesWidth);
                } else {
                    if (maxLines > 0) {
                        textView.setMaxLines(maxLines);
                    }
                }
            } else if (o instanceof CodeView) {
                myAddView((CodeView) o);
                if (useLinesLimit) lines--;
            } else if (o instanceof ImageView) {
                myAddView((ImageView) o);
                if (useLinesLimit) lines--;
            } else if (o instanceof HorizontalScrollView) {
                myAddView((HorizontalScrollView) o);
                if (useLinesLimit) lines--;
            } else if (o instanceof QuoteView) {
                myAddView((QuoteView) o);
                if (useLinesLimit) lines--;
            }
        }
    }

    private void myAddView(View view) {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ;
        if (getChildCount() > 0) {
            layoutParams.topMargin = (int) lineSpacing;
        }
        if (view instanceof FImageView && ((FImageView) view).centered) {
            RelativeLayout rl = new RelativeLayout(mContext);
            RelativeLayout.LayoutParams rlLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlLp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            rl.addView(view);
            rl.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(rl, layoutParams);
        } else {
            addView(view, layoutParams);
        }
    }

    private void resetTokenIndex() {
        mTokenIndex = 0;
    }

    private final Class[] start = {CENTER_START.class, BOLD_START.class, ITALIC_START.class,
            UNDERLINE_START.class, DELETE_START.class, CURTAIN_START.class, TITLE_START.class,
            COLOR_START.class, URL_START.class};

    private final Class[] end = {CENTER_END.class, BOLD_END.class, ITALIC_END.class,
            UNDERLINE_END.class, DELETE_END.class, CURTAIN_END.class, TITLE_END.class,
            COLOR_END.class, URL_END.class};

    private final String CENTER_OP = "center";
    private final String BOLD_OP = "bold";
    private final String ITALIC_OP = "italic";
    private final String UNDERLINE_OP = "underline";
    private final String DELETE_OP = "delete";
    private final String CURTAIN_OP = "curtain";
    private final String TITLE_OP = "title";
    private final String COLOR_OP = "color";
    private final String URL_OP = "url";

    private final String[] operation = {CENTER_OP, BOLD_OP, ITALIC_OP, UNDERLINE_OP, DELETE_OP, CURTAIN_OP, TITLE_OP, COLOR_OP, URL_OP};

    private <T extends TOKEN> List<Object> until(Class<T> endClass) {
        List<Object> ret = new ArrayList<>();

        while (!(thisToken() instanceof END) && !(endClass.isInstance(thisToken()))) {
            boolean flag = false;
            int tmp;

            for (Class anEnd : end) {
                if (anEnd.isInstance(thisToken())) {
                    append(ret, new TextWithFormula(thisToken().value));
                    flag = true;
                    break;
                }
            }

            for (int i = 0; i < start.length; i++) {
                if (start[i].isInstance(thisToken())) {
                    String operand = "";
                    if (thisToken() instanceof CENTER_START) {
                        mCenter = true;
                    } else if (thisToken() instanceof COLOR_START) {
                        operand = ((COLOR_START) thisToken()).color;
                    } else if (thisToken() instanceof URL_START) {
                        operand = ((URL_START) thisToken()).url;
                    }

                    tmp = getTokenIndex();
                    next();
                    List<Object> shown = until(end[i]);
                    mCenter = false;
                    if (shown != null) {
                        concat(ret, operate(shown, operation[i], operand));
                    } else {
                        setTokenIndex(tmp);
                        append(ret, new TextWithFormula(thisToken().value));
                    }
                    flag = true;
                }
            }

            if (!flag) {
                if (thisToken() instanceof PLAIN) {
                    append(ret, new TextWithFormula(thisToken().value));
                } else if (thisToken() instanceof ICON) {
                    final ICON thisToken = (ICON) thisToken();
                    TextWithFormula textWithFormula = new TextWithFormula(thisToken.value);
                    textWithFormula.setSpan(new ImageSpan(mContext, thisToken.iconId), 0,
                            thisToken.value.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    append(ret, textWithFormula);
                } else if (thisToken() instanceof FORMULA) {
                    FORMULA thisToken = (FORMULA) thisToken();
                    TextWithFormula textWithFormula = new TextWithFormula(thisToken().value);
                    textWithFormula.addFormula(0, thisToken.value.length(),
                            thisToken.content, thisToken.contentStart,
                            thisToken.contentStart + thisToken.content.length());
                    append(ret, textWithFormula);
                } else if (thisToken() instanceof CODE_START) {
                    if (!pureText) {
                        tmp = getTokenIndex();
                        int i = 1;
                        StringBuilder string = new StringBuilder("");
                        StringBuilder substring = new StringBuilder("");
                        next();
                        while (!(thisToken() instanceof END)) {
                            if (thisToken() instanceof CODE_START) {
                                i++;
                            }
                            if (thisToken() instanceof CODE_END) {
                                i--;
                                if (i == 0) {
                                    string.append(substring);
                                    break;
                                }
                                string.append(substring);
                                substring.delete(0, substring.length());
                                tmp = getTokenIndex() + 1;
                            }
                            substring.append(thisToken().value);
                            next();
                        }

                        if (i == 0) {
                            final CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
                            codeView.setCode(string.toString());
                            ret.add(codeView);
                        } else if (!TextUtils.isEmpty(string)) {
                            setTokenIndex(tmp);
                            final CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
                            codeView.setCode(string.toString());
                            ret.add(codeView);
                        } else {
                            setTokenIndex(tmp);
                            append(ret, new TextWithFormula(thisToken().value));
                        }
                    } else {
                        append(ret, new TextWithFormula("[代码]"));
                    }
                } else if (thisToken() instanceof IMAGE) {
                    if (!pureText) {
                        IMAGE thisToken = (IMAGE) thisToken();
                        FImageView imageView = loadImage(thisToken.url, thisToken.width, thisToken.height);
                        if (mCenter) {
                            imageView.centered = true;
                        }
                        append(ret, imageView);
                    } else {
                        append(ret, new TextWithFormula("[图片]"));
                    }
                } else if (thisToken() instanceof TABLE) {
                    if (!pureText) {
                        View table = table(thisToken().value);
                        append(ret, table);
                    } else {
                        append(ret, new TextWithFormula("[表格]"));
                    }
                } else if (thisToken() instanceof ATTACHMENT) {
                    final ATTACHMENT thisToken = (ATTACHMENT) thisToken();
                    append(ret, attachment(thisToken.attachment));
                } else if (thisToken() instanceof QUOTE_START) {
                    int i = 1;
                    List<TOKEN> tokens = new ArrayList<>();
                    next();
                    while (!(thisToken() instanceof END)) {
                        if (thisToken() instanceof QUOTE_START) {
                            i++;
                            while (i > 0) {
                                next();
                                if (thisToken() instanceof QUOTE_START) {
                                    i++;
                                } else if (thisToken() instanceof QUOTE_END) {
                                    i--;
                                }
                            }
                        } else if (thisToken() instanceof QUOTE_END) {
                            tokens.add(new END(thisToken().position));
                            break;
                        } else {
                            tokens.add(thisToken());
                        }
                        next();
                    }
                    if (thisToken() instanceof QUOTE_END) {
                        final QuoteView quoteView = QuoteView.newInstance(this, mQuoteViewId);
                        quoteView.setAttachmentList(mAttachmentList);
                        quoteView.setPadding(0, 8, 0, 8);
                        quoteView.setTokens(tokens);
                        quoteView.setOnButtonClickListener(mOnViewClickListener);
                        ret.add(quoteView);
                    } else {
                        append(ret, new TextWithFormula(thisToken().value));
                    }
                }
            }
            next();
        }

        if (endClass.isInstance(thisToken())) {
            return ret;
        }

        return null;
    }

    private Object attachment(final Attachment attachment) {
        if (attachment.isImage()) {
            String url = attachment.getUrl();
            FImageView imageView = loadImage(url);
            if (mCenter) {
                imageView.centered = true;
            }

            return imageView;
        } else {
            TextWithFormula builder = new TextWithFormula(attachment.getText());
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if (mOnViewClickListener != null) {
                        mOnViewClickListener.onAttClick(attachment);
                    }
                }
            }, 0, attachment.getText().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            builder.append("\n\n");

            return builder;
        }
    }

    private void append(List<Object> list, Object element) {
        concat(list, Collections.singletonList(element));
    }

    private FImageView loadImage(String url) {
        return loadImage(url, -1);
    }

    private FImageView loadImage(String url, int size) {
        return loadImage(url, size, size);
    }

    private FImageView loadImage(String url, int width, int height) {
        final FImageView imageView = new FImageView(mContext);

        ViewGroup.LayoutParams layoutParams;

        int phWidth, phHeight, imgWidth, imgHeight;

        if (height != -1 && width != -1) {
            imgHeight = height;
            imgWidth = width;
            phHeight = height;
            phWidth = width;
        } else if (width != -1) {
            imgHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
            imgWidth = width;
            phHeight = MAX_IMAGE_WIDTH / 2;
            phWidth = width;
        } else if (height != -1) {
            imgHeight = height;
            imgWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
            phHeight = height;
            phWidth = MAX_IMAGE_WIDTH;
        } else {
            imgHeight = imgWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
            phHeight = MAX_IMAGE_WIDTH / 2;
            phWidth = MAX_IMAGE_WIDTH;
        }

        if (imageView.centered) {
            layoutParams = new RelativeLayout.LayoutParams(phWidth, phHeight);
            ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        } else {
            layoutParams = new LinearLayout.LayoutParams(phWidth, phHeight);
        }
        imageView.setLayoutParams(layoutParams);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(0, 0, 0, 10);

        final int finalWidth = imgWidth;
        final int finalHeight = imgHeight;

        if (mImageLoader == null) {
            throw new NullPointerException("FlexibleRichTextView has no image loader");
        } else {
            mImageLoader.loadImageDrawable(imageView, url, new FlexibleRichImageLoadCallback() {
                @Override
                public void onDrawableLoaded(Drawable resource) {
                    if (imageView.centered) {
                        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(finalWidth, finalHeight);
                        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        imageView.setLayoutParams(params);
                    } else {
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(finalWidth, finalHeight));
                    }
                    imageView.setImageDrawable(resource);
                    imageView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mOnViewClickListener != null) {
                                mOnViewClickListener.onImgClick(imageView);
                            }
                        }
                    });
                }
            });
        }
        return imageView;
    }

    private List<Object> operate(List<Object> list, String operation, final String... operand) {
        switch (operation) {
            case BOLD_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StyleSpan(Typeface.BOLD), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case CENTER_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else if (o instanceof ImageView) {
                        ((ImageView) o).setScaleType(ImageView.ScaleType.CENTER);
                    }
                }
                break;
            case ITALIC_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StyleSpan(Typeface.ITALIC), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case UNDERLINE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new UnderlineSpan(), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case DELETE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new StrikethroughSpan(), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case CURTAIN_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new BackgroundColorSpan(Color.DKGRAY), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case TITLE_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new RelativeSizeSpan(1.3f), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                break;
            case COLOR_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        try {
                            int color = Color.parseColor(operand[0]);
                            textWithFormula.setSpan(new ForegroundColorSpan(color), 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        } catch (IllegalArgumentException e) {
                            // avoid crash caused by illegal color
                        }
                    }
                }
                break;
            case URL_OP:
                for (Object o : list) {
                    if (o instanceof TextWithFormula) {
                        final TextWithFormula textWithFormula = (TextWithFormula) o;
                        textWithFormula.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(operand[0])));
                            }
                        }, 0, textWithFormula.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
        }
        return list;
    }

    private List<Object> operate(List<Object> list, String operation) {
        return operate(list, operation, "");
    }

    private <T> void concat(List<Object> list1, List<T> list2) {
        if (list1.size() == 0) {
            list1.addAll(list2);
        } else {
            if (list2.size() > 0) {
                if (list1.get(list1.size() - 1) instanceof TextWithFormula &&
                        list2.get(0) instanceof TextWithFormula) {

                    TextWithFormula a = (TextWithFormula) list1.get(list1.size() - 1);
                    TextWithFormula b = (TextWithFormula) list2.get(0);
                    for (TextWithFormula.Formula formula : b.getFormulas()) {
                        formula.start += a.length();
                        formula.end += a.length();
                        formula.contentStart += a.length();
                        formula.contentEnd += a.length();
                    }
                    a.getFormulas().addAll(b.getFormulas());
                    a.append(b);

                    list1.addAll(list2.subList(1, list2.size()));
                } else {
                    list1.addAll(list2);
                }
            }
        }
    }

    private TOKEN thisToken() {
        return mTokenList.get(mTokenIndex);
    }

    private void next() {
        mTokenIndex++;
    }

    public int getTokenIndex() {
        return mTokenIndex;
    }

    public void setTokenIndex(int tokenIndex) {
        this.mTokenIndex = tokenIndex;
    }

    private View table(CharSequence str) {
        final String SPECIAL_CHAR = "\uF487";
        Pattern pattern = Pattern.compile("(?:\\n|^)( *\\|.+\\| *\\n)??( *\\|(?: *:?----*:? *\\|)+ *\\n)((?: *\\|.+\\| *(?:\\n|$))+)");
        Matcher matcher = pattern.matcher(str);
        int[] margins;
        final int LEFT = 0, RIGHT = 1, CENTER = 2;

        if (matcher.find()) {

            List<String> headers = null;
            if (!TextUtils.isEmpty(matcher.group(1))) {
                String wholeHeader = matcher.group(1);

                headers = new ArrayList<>(Arrays.asList(wholeHeader.split("\\|")));
                format(headers);
            }

            List<String> partitions = new ArrayList<>(Arrays.asList(matcher.group(2).split("\\|")));
            format(partitions);
            final int columnNum = partitions.size();
            margins = new int[columnNum];

            for (int i = 0; i < partitions.size(); i++) {
                String partition = partitions.get(i);
                if (partition.startsWith(":") && partition.endsWith(":")) {
                    margins[i] = CENTER;
                } else if (partition.startsWith(":")) {
                    margins[i] = LEFT;
                } else if (partition.endsWith(":")) {
                    margins[i] = RIGHT;
                } else {
                    margins[i] = CENTER;
                }
            }

            String[] rows = matcher.group(3).replace("\\|", SPECIAL_CHAR).split("\n");
            final List<List<String>> content = new ArrayList<>();
            for (String row : rows) {
                content.add(format(new ArrayList<>(Arrays.asList(row.split("\\|")))));
            }

            final List<String[]> whole = new ArrayList<>();
            if (headers != null) {
                whole.add(headers.toArray(new String[columnNum]));
            }
            for (List<String> strings : content) {
                whole.add(strings.toArray(new String[columnNum]));
            }

            // render table
            HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
            TableLayout tableLayout = new TableLayout(mContext);

            tableLayout.addView(getHorizontalDivider());
            for (int i = 0; i < whole.size(); i++) {
                String[] row = whole.get(i);
                TableRow tableRow = new TableRow(mContext);
                final TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(params);

                tableRow.addView(getVerticalDivider());
                for (int j = 0; j < row.length; j++) {
                    String cell = row[j];
                    if (cell != null) {
                        cell = cell.replace(SPECIAL_CHAR, "|");
                    }
                    FlexibleRichTextView flexibleRichTextView = FlexibleRichTextView.newInstance(getContext(), cell, mAttachmentList, mOnViewClickListener, false);
                    TableRow.LayoutParams pcvParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    switch (margins[j]) {
                        case CENTER:
                            pcvParams.gravity = Gravity.CENTER;
                            break;
                        case LEFT:
                            pcvParams.gravity = Gravity.START;
                            break;
                        case RIGHT:
                            pcvParams.gravity = Gravity.END;
                            break;
                    }
                    flexibleRichTextView.setPadding(10, 10, 10, 10);
                    flexibleRichTextView.setLayoutParams(pcvParams);
                    tableRow.addView(flexibleRichTextView);
                    tableRow.addView(getVerticalDivider());
                }
                tableLayout.addView(tableRow);
                tableLayout.addView(getHorizontalDivider());
            }

            scrollView.addView(tableLayout);

            return scrollView;
        }

        return null;

    }


    private List<String> format(List<String> strings) {
        for (int i = strings.size() - 1; i >= 0; i--) {
            String str = strings.get(i);
            if (TextUtils.isEmpty(str) || str.equals("\n")) {
                strings.remove(i);
            }
        }

        for (int i = 0; i < strings.size(); i++) {
            strings.set(i, strings.get(i).trim());
        }

        return strings;
    }

    public static FlexibleRichTextView newInstance(Context context, String string,
                                                   List<Attachment> attachmentList,
                                                   OnViewClickListener onViewClickListener,
                                                   boolean showRemainingAtt) {

        FlexibleRichTextView flexibleRichTextView = new FlexibleRichTextView(context, onViewClickListener, showRemainingAtt);

        if (!TextUtils.isEmpty(string)) {
            flexibleRichTextView.setText(string, attachmentList);
        }

        return flexibleRichTextView;
    }

    private View getHorizontalDivider() {
        View horizontalDivider = new View(mContext);
        horizontalDivider.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        horizontalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return horizontalDivider;
    }

    private View getVerticalDivider() {
        View verticalDivider = new View(mContext);
        verticalDivider.setLayoutParams(new TableRow.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        verticalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return verticalDivider;
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, OnViewClickListener onViewClickListener) {
        init(context, onViewClickListener, true);
    }

    private void init(Context context, OnViewClickListener onViewClickListener, boolean showRemainingAtt) {
        setOrientation(VERTICAL);
        mOnViewClickListener = onViewClickListener;
        mContext = context;
        mShowRemainingAtt = showRemainingAtt;
        removeAllViews();
    }

    public void setOnClickListener(OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
    }

    public int getConversationId() {
        return mConversationId;
    }

    public void setConversationId(int mConversationId) {
        this.mConversationId = mConversationId;
    }

    public void setQuoteViewId(int quoteViewId) {
        this.mQuoteViewId = quoteViewId;
    }

    public interface OnViewClickListener {
        void onViewClick();

        void onImgClick(ImageView imageView);

        void onAttClick(Attachment attachment);

        void onQuoteButtonClick(View view, boolean collapsed);
    }

    public interface OnSetTokenResultCallback {
        void onText(@NonNull SpannableStringBuilder builder);
    }
}
