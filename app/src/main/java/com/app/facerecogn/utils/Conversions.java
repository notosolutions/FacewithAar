package com.app.facerecogn.utils;

public class Conversions {

  private static Conversions mCom=null;

  public static Conversions getInstance(){
    if(mCom==null){
        mCom=new Conversions();
    }
    return mCom;
  }

  public native double appTestJni(String sInputPath, String sOutputPath);
  public native double SpotCRJni(String sInputPath, String sOutputPath, int cx, int cy, int pw, int ph);
  public native String SkintoneCRJni(String sInputPath);
  public native double PoresCRJni(String sInputPath, String sOutputPath, String sTextPath, int cx, int cy, int pw, int ph);

  static {
    System.load("mylibrary-debug");
  }
}