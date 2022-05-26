# From https://gist.github.com/clarketm/dc5d5be390e3f811a2dd7f5e8c5728ba

import os,sys
from oauth2client.client import OAuth2WebServerFlow
from oauth2client.tools import run_flow
from oauth2client.file import Storage

from dotenv import load_dotenv

load_dotenv()

def disable_stout():
	o_stdout = sys.stdout
	o_file = open(os.devnull, "w")
	sys.stdout = o_file
	return (o_stdout, o_file)

def enable_stout(o_stdout, o_file):
	o_file.close()
	sys.stdout = o_stdout

def getToken():
	CLIENT_ID = os.getenv('CLIENT_ID')
	CLIENT_SECRET = os.getenv('CLIENT_SECRET')
	SCOPE = 'https://www.googleapis.com/auth/gmail.send'
	REDIRECT_URL = 'http://127.0.0.1'
	
	o_stdout, o_file = disable_stout()
	
	flow = OAuth2WebServerFlow(client_id=CLIENT_ID, client_secret=CLIENT_SECRET, scope=SCOPE, redirect_uri=REDIRECT_URL)
	
	storage = Storage('creds.data')
	credentials = run_flow(flow, storage)
	enable_stout(o_stdout, o_file)
	
#	print ("access_token: %s" % (credentials.access_token))
	print(credentials.access_token)
	
getToken()