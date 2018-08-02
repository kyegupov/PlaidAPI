package demo.controller;

import com.plaid.client.request.*;
import com.plaid.client.request.common.Product;
import com.plaid.client.response.*;
import demo.service.PlaidAuthService;
import com.plaid.client.PlaidClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import retrofit2.Response;

import java.util.*;


/**
 * Created by ryandesmond on 8/1/18.
 */

@RestController
@RequestMapping("/test")
public class TestController {

    private final Environment env;
    private PlaidClient plaidClient;
    private final PlaidAuthService authService;


    @Autowired
    public TestController(Environment env, PlaidClient plaidClient, PlaidAuthService authService) {
        this.env = env;
        this.plaidClient = plaidClient;
        this.authService = authService;
    }


    @GetMapping(value="/")
    public String index(Model model) {
        model.addAttribute("PLAID_PUBLIC_KEY", env.getProperty("PLAID_PUBLIC_KEY"));
        model.addAttribute("PLAID_ENV", env.getProperty("PLAID_ENV"));
        return "index";
    }


    public void getAccessToken() throws Exception {

        String accessToken;

        plaidClient = PlaidClient.newBuilder()
                .clientIdAndSecret("5b51290f4ca9fb0011c5bffe", "846f197e0e89aac5d4e8dcf484c484")
                .publicKey("3b6e5c84bf8feb3dda6cfdd2f9ff72") // optional. only needed to call endpoints that require a public key
                .sandboxBaseUrl() // or equivalent, depending on which environment you're calling into
                .build();

        Response<SandboxPublicTokenCreateResponse> createResponse = plaidClient.service()
                .sandboxPublicTokenCreate(new SandboxPublicTokenCreateRequest("ins_109511", Arrays.asList(Product.AUTH)))
                .execute();


        // Synchronously exchange a Link public_token for an API access_token
        // Required request parameters are always Request object constructor arguments
        Response<ItemPublicTokenExchangeResponse> response = plaidClient.service()
                .itemPublicTokenExchange(new ItemPublicTokenExchangeRequest(createResponse.body().getPublicToken()))
                .execute();

        if (response.isSuccessful()) {
            accessToken = response.body().getAccessToken();
            System.out.println(accessToken);
            this.authService.setAccessToken(response.body().getAccessToken());
            this.authService.setItemId(response.body().getItemId());


            Map<String, Object> data = new HashMap<>();
            data.put("error", false);

        } else {
            System.out.println(response.errorBody().string());
        }
    }

    @RequestMapping("/item")
    public @ResponseBody
    ResponseEntity getItem() throws Exception {
        getAccessToken();

        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        //calls the ItemGetRequest which takes in an access token and returns a response of same type
        Response<ItemGetResponse> itemResponse = this.plaidClient.service()
                .itemGet(new ItemGetRequest(this.authService.getAccessToken()))
                .execute();

        //if the reponse doesnt work then print out an error message
        //otherwise if all good then set object item to a body of the response class which is a URL
        if (!itemResponse.isSuccessful()) {
            System.out.println(itemResponse.errorBody().string());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(getErrorResponseData("Unable to pull item information from the Plaid API."));
        } else {
            ItemStatus item = itemResponse.body().getItem();

            Response<InstitutionsGetByIdResponse> institutionsResponse = this.plaidClient.service()
                    .institutionsGetById(new InstitutionsGetByIdRequest(item.getInstitutionId()))
                    .execute();

                Map<String, Object> data = new HashMap<>();
                data.put("error", false);
                data.put("item", item);
                data.put("institution", institutionsResponse.body().getInstitution());
                return ResponseEntity.ok(data);
            }
        }




    @GetMapping(value = "/accounts")
    public @ResponseBody
    ResponseEntity getAccount() throws Exception {
        getAccessToken();

        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        Response<AccountsGetResponse> response = this.plaidClient.service()
                .accountsGet(new AccountsGetRequest(this.authService.getAccessToken())).execute();

        if (response.isSuccessful()) {
            Map<String, Object> data = new HashMap<>();
            data.put("error", false);
            data.put("accounts", response.body().getAccounts());
            data.put("numbers", response.body().getItem());

            return ResponseEntity.ok(data);
        } else {
            System.out.println(response.errorBody().string());
            Map<String, Object> data = new HashMap<>();
            data.put("error", "Unable to pull accounts from the Plaid API.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
        }
    }




    @RequestMapping(value = "/transactions", produces=MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity getTransactions() throws Exception {
        getAccessToken();

        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        //setting the timescale
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        Date startDate = cal.getTime();
        Date endDate = new Date();

        ArrayList<String> list = new ArrayList<>();
        list.add("gEJG9ajonlhN9N7qggeQtEVMoRp8EbSgXQrkW");
        list.add("olLGEmpvqdHk3kg5MMrnCJbqdZEGJrtR3nrwg");
        list.add("E8rpeBMJmlTGPG1gpp53tVy4B5NvVwiXLPVdP");
        list.add("Wa47Ny63jqS6L6Vz335ytDBJANZzDdFlG7vaK");
        list.add("8NRdKjBgMyHrXr1VaadbtldgJwB5lefwyVzEG");

        Response<TransactionsGetResponse> response = this.plaidClient.service()
                .transactionsGet(new TransactionsGetRequest(this.authService.getAccessToken(), startDate, endDate)
                        .withCount(250)
                        .withAccountIds(list)
                        .withOffset(0))
                .execute();
        if (response.isSuccessful()) {
            return ResponseEntity.ok(response.body());
        } else {
            System.out.println(response.errorBody().string());
            ErrorResponse error = this.plaidClient.parseError(response);
            Map<String, Object> data = new HashMap<>();
            data.put("error", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
        }
    }



    private Map<String, Object> getErrorResponseData(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("error", false);
        data.put("message", message);
        return data;
    }
}

