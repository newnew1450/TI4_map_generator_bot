package ti4.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Installation {
    private String tile;
    private boolean exhausted;
    private InstallationModel model;
}
