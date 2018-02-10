package cn.com.protruly.filemanager.globalsearch;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GlobalSearchHisDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "globalsearch";
    public static final String VOLUMN_ID = "_id";
    public static final String TABLE_NAME = "records";
    public static final String VOLUMN_NAME = "name";
    private static Integer version = 1;

    public GlobalSearchHisDbHelper(Context context) {
        super(context, TABLE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS "
                +TABLE_NAME
                +"("
                +VOLUMN_ID
                +" INTEGER PRIMARY KEY AUTOINCREMENT,"
                +VOLUMN_NAME
                +" TEXT UNIQUE)";
        db.execSQL(sql);
        //db.execSQL("create table records(id integer primary key autoincrement,name varchar(200))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}