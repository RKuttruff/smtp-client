# smtp-client

A simple SMTP client created as a class project. For now, it can only interact with GMail, but can send to any valid address.

## Usage

`java SMTPClient -type=raw|cli|gui [OPTIONS...]`

`java SMTPClient -type=file [-v] [-from=<usr gmail addr>] [-to=<rcpt addr>[(;<rcpt addr>)*]] [-pass=<usr passwd>] -- FILE...`

## Options
<pre>
-v
Prints verbose output.

-type=raw|cli|gui|file
    Sets the type of client.
      -raw:   Manual SMTP interaction
      -cli:   Command-Line interface (THIS HAS NOT BEEN IMPLEMENTED)
      -gui:   Graphical user interface
      -file:  Reads message data from files & standard input (in a similar fashion to cat).
              The list of files MUST appear after --. Filename - indicates reading from
              standard input. This type REQUIRES setting the user address and recipient
              addresses as arguments. The user password MAY also be set. If not,
              however, a GUI dialog will be used to prompt for the password.

-from=&lt;address&gt;
    Sets the gmail address of the user. If unset, user will be prompted at runtime.

-to=&lt;addr&gt;[(;&lt;addr&gt;)*]
    Sets the address(es) of the recipients. If unset, user will be prompted at runtime. For
    multiple recipients, separate addresses by semicolons.

-pass=&lt;password&gt;
    Sets user's gmail password. If unset, user will be prompted at runtime. It is
    recommended that this not be used to avoid the password being stored in an
    immutable String object, rather than using a clearable character array.

-auth=&lt;AUTH method&gt;
    Sets the authentication method to use. Consult option -list-auth for valid options.

-list-auth
    Prints all implemented AUTH methods, then exits.

-help
    Prints this message, then exits.
</pre>

## Exit Values

| Value | Description                                                     |
|-------|-----------------------------------------------------------------|
|     0 | OK                                                              |
|     1 | Could not resolve SMTP host                                     |
|     2 | Could not connect to SMTP server                                |
|     3 | IO error                                                        |
|     4 | SMTP authentication failed                                      |
|     5 | Invalid command line option                                     |
|     6 | Bad command line                                                |
|     7 | No valid recipient addresses                                    |
|     8 | GUI requested but not supported by the runtime environment.     |
|     9 | Authentication info file (.env) not found.                      |
|    10 | Authentication info file (.env) does not contain needed fields. |
|    11 | Authentication subprocess failure.                              |
|    12 | No valid authentication methods.                                |
|   404 | File not found. (for type=file)                                 |
|    -1 | Feature not implemented                                         |
| Other | SMTP error code (4xx, 5xx)                                      |

## Currently Implemented Authentication Methods

- PLAIN
- XOAUTH2

### Setup for XOAUTH2 Method Functionality

1. See [this guide](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid "Google API Project Guide") to create a Google API Project. Ensure you have the GMail API enabled in your project.
2. Create a file `.env` in the directory you intend to have the client.
3. Edit the file to add the following information:
<pre>
   CLIENT_ID=Your client ID
   CLIENT_SECRET=Your client secret
</pre>
