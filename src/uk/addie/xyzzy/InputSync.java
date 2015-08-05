package uk.addie.xyzzy;

public class InputSync {
  private int character = 0;

  private String string = null;

  public void setCharacter(final int character) {
    this.character = character;
  }

  public void setString(final String string) {
    this.string = string;
  }

  public String string() {
    return string;
  }

  int character() {
    return character;
  }
}
